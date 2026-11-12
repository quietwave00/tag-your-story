package com.tagnote.application.enrichment.provider;

import com.tagnote.application.catalog.importer.model.ImportedArtist;
import com.tagnote.application.catalog.importer.model.ImportedAlbum;
import com.tagnote.application.catalog.importer.model.ImportedTrack;
import com.tagnote.application.enrichment.config.ExternalEnrichmentProperties;
import com.tagnote.application.enrichment.diagnostics.EnrichmentDiagnostics;
import com.tagnote.application.enrichment.exception.ExternalProviderException;
import com.tagnote.application.enrichment.matching.LastFmEntityMatchingService;
import com.tagnote.application.enrichment.matching.MusicEditionTitleNormalizer;
import com.tagnote.application.enrichment.matching.MusicEntityNameNormalizer;
import com.tagnote.application.enrichment.matching.model.LastFmCatalogData.Tag;
import com.tagnote.application.enrichment.matching.model.LastFmCatalogData.TopTags;
import com.tagnote.application.enrichment.model.CatalogExternalIdentityMatch;
import com.tagnote.application.enrichment.model.CollectedExternalTags;
import com.tagnote.application.enrichment.model.ExternalTagInput;
import com.tagnote.application.enrichment.model.EnrichmentDeadline;
import com.tagnote.application.enrichment.model.ProviderEnrichmentResult;
import com.tagnote.application.enrichment.model.ProviderEnrichmentStatus;
import com.tagnote.application.enrichment.port.ExternalTagProvider;
import com.tagnote.application.enrichment.port.LastFmCatalogClient;
import com.tagnote.domain.enrichment.assertion.EvidenceType;
import com.tagnote.domain.enrichment.observation.ExternalTagSource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;

@Order(300)
@Component
@ConditionalOnProperty(prefix = "tag.enrichment.lastfm", name = "enabled", havingValue = "true")
public class LastFmExternalTagProvider implements ExternalTagProvider {

    private final LastFmCatalogClient client;
    private final LastFmEntityMatchingService matchingService;
    private final MusicEditionTitleNormalizer editionTitleNormalizer;
    private final MusicEntityNameNormalizer normalizer;
    private final ExternalEnrichmentProperties.LastFm config;
    private final ExternalEnrichmentProperties.EvidenceConfidence confidence;

    public LastFmExternalTagProvider(
            LastFmCatalogClient client,
            LastFmEntityMatchingService matchingService,
            MusicEditionTitleNormalizer editionTitleNormalizer,
            MusicEntityNameNormalizer normalizer,
            ExternalEnrichmentProperties properties
    ) {
        this.client = client;
        this.matchingService = matchingService;
        this.editionTitleNormalizer = editionTitleNormalizer;
        this.normalizer = normalizer;
        this.config = properties.getLastfm();
        this.confidence = properties.getEvidenceConfidence();
    }

    @Override
    public ExternalTagSource source() {
        return ExternalTagSource.LASTFM;
    }

    @Override
    public ProviderEnrichmentResult collect(ImportedTrack track, EnrichmentDeadline deadline) {
        ExternalProviderException firstFailure = null;
        List<ExternalTagInput> trackInputs = List.of();
        List<ExternalTagInput> albumInputs = List.of();
        boolean trackMatched = false;
        boolean albumMatched = false;

        String trackArtist = representativeArtist(track.getArtists());
        if (trackArtist != null) {
            try {
                TopTags response = exactTrackTopTags(trackArtist, track.getTitle());
                if (response.tags().isEmpty()) {
                    String canonicalTitle = editionTitleNormalizer.canonicalTitle(track.getTitle());
                    if (!canonicalTitle.equals(track.getTitle())) {
                        EnrichmentDiagnostics.event("lastfm_fallback", java.util.Map.of(
                                "subject", "TRACK", "from", track.getTitle(), "to", canonicalTitle,
                                "reason", "ORIGINAL_EMPTY"));
                        response = exactTrackTopTags(trackArtist, canonicalTitle);
                    }
                }
                trackMatched = true;
                trackInputs = inputs(
                        response.tags(),
                        externalRef("track", trackArtist, track.getTitle()),
                        confidence.getLastfmTrackCommunityTag()
                );
            } catch (ExternalProviderException failure) {
                firstFailure = failure;
            }
        }

        String albumArtist = representativeArtist(track.getAlbum().getArtists());
        if (albumArtist != null) {
            try {
                TopTags response = client.getAlbumTopTags(albumArtist, track.getAlbum().getTitle());
                if (matchingService.matches(albumArtist, track.getAlbum().getTitle(), response)) {
                    EnrichmentDiagnostics.event("lastfm_identity", java.util.Map.of(
                            "subject", "ALBUM", "matched", true));
                    albumMatched = true;
                    albumInputs = inputs(
                            response.tags(),
                            externalRef("album", albumArtist, track.getAlbum().getTitle()),
                            confidence.getLastfmAlbumCommunityTag()
                    );
                } else if (firstFailure == null) {
                    EnrichmentDiagnostics.event("lastfm_identity", java.util.Map.of(
                            "subject", "ALBUM", "matched", false));
                    firstFailure = new ExternalProviderException(
                            ProviderEnrichmentStatus.NOT_FOUND,
                            "Last.fm Album identity did not match exactly"
                    );
                }
            } catch (ExternalProviderException failure) {
                if (firstFailure == null) {
                    firstFailure = failure;
                }
            }
        }

        if (!trackMatched && !albumMatched && firstFailure != null) {
            throw firstFailure;
        }
        return ProviderEnrichmentResult.completed(
                source(),
                new CollectedExternalTags(albumInputs, trackInputs),
                CatalogExternalIdentityMatch.none()
        );
    }

    @Override
    public ProviderEnrichmentResult collectTrackDirect(ImportedTrack track, EnrichmentDeadline deadline) {
        String artist = representativeArtist(track.getArtists());
        if (artist == null) {
            throw new ExternalProviderException(ProviderEnrichmentStatus.NOT_FOUND,
                    "Last.fm Track lookup requires a Track Artist");
        }
        TopTags response = exactTrackTopTags(artist, track.getTitle());
        if (response.tags().isEmpty()) {
            String canonicalTitle = editionTitleNormalizer.canonicalTitle(track.getTitle());
            if (!canonicalTitle.equals(track.getTitle())) {
                EnrichmentDiagnostics.event("lastfm_fallback", java.util.Map.of(
                        "subject", "TRACK", "from", track.getTitle(), "to", canonicalTitle,
                        "reason", "ORIGINAL_EMPTY"));
                response = exactTrackTopTags(artist, canonicalTitle);
            }
        }
        return ProviderEnrichmentResult.completed(source(),
                new CollectedExternalTags(List.of(), inputs(response.tags(),
                        externalRef("track", artist, track.getTitle()),
                        confidence.getLastfmTrackCommunityTag())), CatalogExternalIdentityMatch.none());
    }

    @Override
    public ProviderEnrichmentResult collectAlbum(ImportedAlbum album, EnrichmentDeadline deadline) {
        String artist = representativeArtist(album.getArtists());
        if (artist == null) {
            throw new ExternalProviderException(ProviderEnrichmentStatus.NOT_FOUND,
                    "Last.fm Album lookup requires an Album Artist");
        }
        TopTags response = client.getAlbumTopTags(artist, album.getTitle());
        if (!matchingService.matches(artist, album.getTitle(), response)) {
            EnrichmentDiagnostics.event("lastfm_identity", java.util.Map.of(
                    "subject", "ALBUM", "matched", false));
            throw new ExternalProviderException(ProviderEnrichmentStatus.NOT_FOUND,
                    "Last.fm Album identity did not match exactly");
        }
        EnrichmentDiagnostics.event("lastfm_identity", java.util.Map.of(
                "subject", "ALBUM", "matched", true));
        return ProviderEnrichmentResult.completed(source(),
                new CollectedExternalTags(inputs(response.tags(),
                        externalRef("album", artist, album.getTitle()),
                        confidence.getLastfmAlbumCommunityTag()), List.of()),
                CatalogExternalIdentityMatch.none());
    }

    private TopTags exactTrackTopTags(String artist, String title) {
        TopTags response = client.getTrackTopTags(artist, title);
        if (!matchingService.matches(artist, title, response)) {
            EnrichmentDiagnostics.event("lastfm_identity", java.util.Map.of(
                    "subject", "TRACK", "matched", false));
            throw new ExternalProviderException(
                    ProviderEnrichmentStatus.NOT_FOUND,
                    "Last.fm Track identity did not match exactly"
            );
        }
        EnrichmentDiagnostics.event("lastfm_identity", java.util.Map.of(
                "subject", "TRACK", "matched", true));
        return response;
    }

    private List<ExternalTagInput> inputs(List<Tag> tags, String externalRef, double value) {
        LinkedHashMap<String, ExternalTagInput> unique = new LinkedHashMap<>();
        List<java.util.Map<String, Object>> excluded = EnrichmentDiagnostics.enabled()
                ? new java.util.ArrayList<>() : null;
        for (int rank = 0; rank < tags.size(); rank++) {
            Tag tag = tags.get(rank);
            if (unique.size() >= config.getMaximumTagsPerSubject()) {
                if (excluded == null) break;
                if (excluded != null) excluded.add(java.util.Map.of("rank", rank + 1,
                        "name", EnrichmentDiagnostics.safe(tag.name()), "reason", "LIMIT"));
                continue;
            }
            if (tag.name() == null || tag.name().isBlank()) {
                if (excluded != null) excluded.add(java.util.Map.of("rank", rank + 1,
                        "name", EnrichmentDiagnostics.safe(tag.name()), "reason", "BLANK"));
                continue;
            }
            String normalized = normalizer.normalize(tag.name());
            if (excluded != null && unique.containsKey(normalized)) excluded.add(java.util.Map.of(
                    "rank", rank + 1, "name", tag.name(), "reason", "DUPLICATE"));
            unique.putIfAbsent(
                    normalized,
                    new ExternalTagInput(
                            source(), tag.name(), externalRef, EvidenceType.COMMUNITY_TAG, value
                    )
            );
        }
        if (EnrichmentDiagnostics.enabled()) EnrichmentDiagnostics.event("lastfm_tag_selection", java.util.Map.of(
                "rawCount", tags.size(), "selectedCount", unique.size(),
                "selected", EnrichmentDiagnostics.bounded(unique.values().stream()
                        .map(ExternalTagInput::rawName).toList()),
                "excluded", EnrichmentDiagnostics.bounded(excluded)));
        return List.copyOf(unique.values());
    }

    private String representativeArtist(List<ImportedArtist> artists) {
        return artists.stream()
                .filter(artist -> artist.getPosition() == 0)
                .map(ImportedArtist::getName)
                .findFirst()
                .orElse(null);
    }

    private String externalRef(String subject, String artist, String title) {
        String identity = subject + "\u0000" + normalizer.normalize(artist) + "\u0000" + normalizer.normalize(title);
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(identity.getBytes(StandardCharsets.UTF_8));
            return "lastfm:" + subject + ":" + HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is not available", impossible);
        }
    }
}
