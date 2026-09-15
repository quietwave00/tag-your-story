package com.tagnote.application.enrichment.provider;

import com.tagnote.application.catalog.importer.model.ImportedAlbum;
import com.tagnote.application.catalog.importer.model.ImportedArtist;
import com.tagnote.application.catalog.importer.model.ImportedTrack;
import com.tagnote.application.enrichment.config.ExternalEnrichmentProperties;
import com.tagnote.application.enrichment.exception.ExternalProviderException;
import com.tagnote.application.enrichment.matching.DiscogsAlbumMatchingService;
import com.tagnote.application.enrichment.matching.MusicEntityNameNormalizer;
import com.tagnote.application.enrichment.matching.model.DiscogsCatalogData.AlbumCandidate;
import com.tagnote.application.enrichment.matching.model.DiscogsCatalogData.AlbumDetails;
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
    private final ExternalEnrichmentProperties.EvidenceConfidence confidence;
    private final MusicEntityNameNormalizer normalizer;

    public DiscogsExternalTagProvider(
            DiscogsCatalogClient client,
            DiscogsAlbumMatchingService matchingService,
            MusicEntityNameNormalizer normalizer,
            ExternalEnrichmentProperties properties
    ) {
        this.client = client;
        this.matchingService = matchingService;
        this.normalizer = normalizer;
        this.confidence = properties.getEvidenceConfidence();
    }

    @Override
    public ExternalTagSource source() {
        return ExternalTagSource.DISCOGS;
    }

    @Override
    public ProviderEnrichmentResult collect(ImportedTrack track, EnrichmentDeadline deadline) {
        ImportedAlbum album = track.getAlbum();
        List<String> artists = album.getArtists().stream().map(ImportedArtist::getName).toList();
        if (artists.isEmpty()) {
            throw new ExternalProviderException(
                    ProviderEnrichmentStatus.NOT_FOUND,
                    "Discogs matching requires an Album Artist"
            );
        }
        AlbumCandidate matched = findAlbum(album, artists);
        AlbumDetails details = client.getAlbum(matched.type(), matched.id());
        if (details.id() != matched.id()
                || details.type() != matched.type()
                || !matchingService.validates(album, details)) {
            throw new ExternalProviderException(
                    ProviderEnrichmentStatus.NOT_FOUND,
                    "Discogs Album detail did not pass exact validation"
            );
        }

        String prefix = matched.type() == EntityType.MASTER ? "master" : "release";
        String externalRef = "discogs:" + prefix + ":" + matched.id();
        List<ExternalTagInput> inputs = new java.util.ArrayList<>();
        inputs.addAll(inputs(details.genres(), externalRef, EvidenceType.EXPLICIT_GENRE, confidence.getDiscogsGenre()));
        inputs.addAll(inputs(details.styles(), externalRef, EvidenceType.EXPLICIT_STYLE, confidence.getDiscogsStyle()));
        return ProviderEnrichmentResult.completed(
                source(),
                new CollectedExternalTags(inputs, List.of()),
                CatalogExternalIdentityMatch.none()
        );
    }

    private AlbumCandidate findAlbum(ImportedAlbum album, List<String> artists) {
        String searchTitle = matchingService.searchTitle(album);
        List<AlbumCandidate> masterMatches = matchingService.matchingCandidates(
                album,
                client.searchAlbums(searchTitle, artists, EntityType.MASTER)
        );
        if (masterMatches.size() == 1) {
            return masterMatches.get(0);
        }
        if (masterMatches.size() > 1) {
            throw notUniquelyMatched("masterMatches=" + masterMatches.size());
        }

        List<AlbumCandidate> releaseMatches = matchingService.matchingCandidates(
                album,
                client.searchAlbums(searchTitle, artists, EntityType.RELEASE)
        );
        if (releaseMatches.size() == 1) {
            return releaseMatches.get(0);
        }
        throw notUniquelyMatched(
                "masterMatches=0, releaseMatches=" + releaseMatches.size()
        );
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
}
