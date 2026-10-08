package com.tagnote.application.enrichment.provider;

import com.tagnote.application.catalog.importer.model.ImportedAlbum;
import com.tagnote.application.catalog.importer.model.ImportedArtist;
import com.tagnote.application.catalog.importer.model.ImportedTrack;
import com.tagnote.application.enrichment.config.ExternalEnrichmentProperties;
import com.tagnote.application.enrichment.exception.ExternalProviderException;
import com.tagnote.application.enrichment.matching.DiscogsAlbumMatchingService;
import com.tagnote.application.enrichment.matching.DiscogsAlbumSearchPlan;
import com.tagnote.application.enrichment.matching.MusicEntityNameNormalizer;
import com.tagnote.application.enrichment.matching.model.DiscogsCatalogData.AlbumCandidate;
import com.tagnote.application.enrichment.matching.model.DiscogsCatalogData.AlbumDetails;
import com.tagnote.application.enrichment.matching.model.DiscogsCatalogData.AlbumSearchAttempt;
import com.tagnote.application.enrichment.matching.model.DiscogsCatalogData.EntityType;
import com.tagnote.application.enrichment.model.CatalogExternalIdentityMatch;
import com.tagnote.application.enrichment.model.CollectedExternalTags;
import com.tagnote.application.enrichment.model.ExternalTagInput;
import com.tagnote.application.enrichment.model.EnrichmentDeadline;
import com.tagnote.application.enrichment.model.ProviderEnrichmentResult;
import com.tagnote.application.enrichment.model.ProviderEnrichmentStatus;
import com.tagnote.application.enrichment.port.ExternalTagProvider;
import com.tagnote.application.enrichment.port.DiscogsCatalogClient;
import com.tagnote.domain.enrichment.assertion.EvidenceType;
import com.tagnote.domain.enrichment.observation.ExternalTagSource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;

@Order(200)
@Component
@ConditionalOnProperty(prefix = "tag.enrichment.discogs", name = "enabled", havingValue = "true")
public class DiscogsExternalTagProvider implements ExternalTagProvider {

    private final DiscogsCatalogClient client;
    private final DiscogsAlbumMatchingService matchingService;
    private final DiscogsAlbumSearchPlan searchPlan;
    private final ExternalEnrichmentProperties.EvidenceConfidence confidence;
    private final MusicEntityNameNormalizer normalizer;

    public DiscogsExternalTagProvider(
            DiscogsCatalogClient client,
            DiscogsAlbumMatchingService matchingService,
            DiscogsAlbumSearchPlan searchPlan,
            MusicEntityNameNormalizer normalizer,
            ExternalEnrichmentProperties properties
    ) {
        this.client = client;
        this.matchingService = matchingService;
        this.searchPlan = searchPlan;
        this.normalizer = normalizer;
        this.confidence = properties.getEvidenceConfidence();
    }

    @Override
    public ExternalTagSource source() {
        return ExternalTagSource.DISCOGS;
    }

    @Override
    public ProviderEnrichmentResult collect(ImportedTrack track, EnrichmentDeadline deadline) {
        return collectAlbum(track.getAlbum(), searchPlan.attempts(track));
    }

    @Override
    public ProviderEnrichmentResult collectAlbum(ImportedAlbum album, EnrichmentDeadline deadline) {
        return collectAlbum(album, searchPlan.attempts(album));
    }

    private ProviderEnrichmentResult collectAlbum(ImportedAlbum album, List<AlbumSearchAttempt> attempts) {
        List<String> artists = album.getArtists().stream().map(ImportedArtist::getName).toList();
        if (artists.isEmpty()) {
            throw new ExternalProviderException(
                    ProviderEnrichmentStatus.NOT_FOUND,
                    "Discogs matching requires an Album Artist"
            );
        }
        MatchedAlbum matched = findAlbum(album, attempts);
        AlbumDetails details = client.getAlbum(matched.candidate().type(), matched.candidate().id());
        if (details.id() != matched.candidate().id()
                || details.type() != matched.candidate().type()
                || !matchingService.validates(album, matched.validationTitle(), details)) {
            throw new ExternalProviderException(
                    ProviderEnrichmentStatus.NOT_FOUND,
                    "Discogs Album detail did not pass exact validation"
            );
        }

        String prefix = matched.candidate().type() == EntityType.MASTER ? "master" : "release";
        String externalRef = "discogs:" + prefix + ":" + matched.candidate().id();
        List<ExternalTagInput> inputs = new java.util.ArrayList<>();
        inputs.addAll(inputs(details.genres(), externalRef, EvidenceType.EXPLICIT_GENRE, confidence.getDiscogsGenre()));
        inputs.addAll(inputs(details.styles(), externalRef, EvidenceType.EXPLICIT_STYLE, confidence.getDiscogsStyle()));
        return ProviderEnrichmentResult.completed(
                source(),
                new CollectedExternalTags(inputs, List.of()),
                CatalogExternalIdentityMatch.none()
        );
    }

    private MatchedAlbum findAlbum(ImportedAlbum album, List<AlbumSearchAttempt> attempts) {
        for (AlbumSearchAttempt attempt : attempts) {
            MatchedAlbum matched = findAlbum(album, attempt);
            if (matched != null) {
                return matched;
            }
        }
        throw notUniquelyMatched("all search attempts returned zero matches");
    }

    private MatchedAlbum findAlbum(ImportedAlbum album, AlbumSearchAttempt attempt) {
        List<AlbumCandidate> masterMatches = matchingService.matchingCandidates(
                album,
                attempt.validationTitle(),
                client.searchAlbums(attempt.query(), EntityType.MASTER)
        );
        if (masterMatches.size() == 1) {
            return new MatchedAlbum(masterMatches.get(0), attempt.validationTitle());
        }
        if (masterMatches.size() > 1) {
            throw notUniquelyMatched(attemptDetail(attempt, "masterMatches=" + masterMatches.size()));
        }

        List<AlbumCandidate> releaseMatches = matchingService.matchingCandidates(
                album,
                attempt.validationTitle(),
                client.searchAlbums(attempt.query(), EntityType.RELEASE)
        );
        if (releaseMatches.size() == 1) {
            return new MatchedAlbum(releaseMatches.get(0), attempt.validationTitle());
        }
        if (releaseMatches.size() > 1) {
            throw notUniquelyMatched(attemptDetail(
                    attempt,
                    "masterMatches=0, releaseMatches=" + releaseMatches.size()
            ));
        }
        return null;
    }

    private String attemptDetail(AlbumSearchAttempt attempt, String matches) {
        return "field=" + attempt.query().field()
                + ", value=" + attempt.query().value()
                + ", " + matches;
    }

    private ExternalProviderException notUniquelyMatched(String detail) {
        return new ExternalProviderException(
                ProviderEnrichmentStatus.NOT_FOUND,
                "Discogs Album was not uniquely matched. " + detail
        );
    }

    private List<ExternalTagInput> inputs(
            List<String> names,
            String externalRef,
            EvidenceType evidenceType,
            double value
    ) {
        LinkedHashMap<String, ExternalTagInput> unique = new LinkedHashMap<>();
        for (String name : names) {
            if (name == null || name.isBlank()) {
                continue;
            }
            unique.putIfAbsent(normalizer.normalize(name), new ExternalTagInput(
                    source(), name, externalRef, evidenceType, value
            ));
        }
        return List.copyOf(unique.values());
    }

    private record MatchedAlbum(AlbumCandidate candidate, String validationTitle) {
    }
}
