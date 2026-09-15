package com.tagnote.application.enrichment.provider;

import com.tagnote.application.catalog.importer.model.ImportedArtist;
import com.tagnote.application.catalog.importer.model.ImportedTrack;
import com.tagnote.application.enrichment.config.ExternalEnrichmentProperties;
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
                    albumMatched = true;
                    albumInputs = inputs(
                            response.tags(),
                            externalRef("album", albumArtist, track.getAlbum().getTitle()),
                            confidence.getLastfmAlbumCommunityTag()
                    );
                } else if (firstFailure == null) {
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

    private TopTags exactTrackTopTags(String artist, String title) {
        TopTags response = client.getTrackTopTags(artist, title);
        if (!matchingService.matches(artist, title, response)) {
            throw new ExternalProviderException(
                    ProviderEnrichmentStatus.NOT_FOUND,
                    "Last.fm Track identity did not match exactly"
            );
        }
        return response;
    }

    private List<ExternalTagInput> inputs(List<Tag> tags, String externalRef, double value) {
        LinkedHashMap<String, ExternalTagInput> unique = new LinkedHashMap<>();
        for (Tag tag : tags) {
            if (unique.size() >= config.getMaximumTagsPerSubject()) {
                break;
            }
            if (tag.name() == null || tag.name().isBlank()) {
                continue;
            }
            unique.putIfAbsent(
                    normalizer.normalize(tag.name()),
                    new ExternalTagInput(
                            source(), tag.name(), externalRef, EvidenceType.COMMUNITY_TAG, value
                    )
            );
        }
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
