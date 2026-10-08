package com.tagnote.application.enrichment.provider;

import com.tagnote.application.catalog.importer.model.ImportedArtist;
import com.tagnote.application.catalog.importer.model.ImportedAlbum;
import com.tagnote.application.catalog.importer.model.ImportedTrack;
import com.tagnote.application.enrichment.config.ExternalEnrichmentProperties;
import com.tagnote.application.enrichment.exception.ExternalProviderException;
import com.tagnote.application.enrichment.matching.MusicBrainzEntityMatchingService;
import com.tagnote.application.enrichment.matching.MusicEditionTitleNormalizer;
import com.tagnote.application.enrichment.matching.MusicEntityNameNormalizer;
import com.tagnote.application.enrichment.matching.model.MusicBrainzCatalogData.Genre;
import com.tagnote.application.enrichment.matching.model.MusicBrainzCatalogData.RecordingCandidate;
import com.tagnote.application.enrichment.matching.model.MusicBrainzCatalogData.RecordingDetails;
import com.tagnote.application.enrichment.matching.model.MusicBrainzCatalogData.ReleaseGroupCandidate;
import com.tagnote.application.enrichment.matching.model.MusicBrainzCatalogData.ReleaseGroupDetails;
import com.tagnote.application.enrichment.model.CatalogExternalIdentityMatch;
import com.tagnote.application.enrichment.model.CollectedExternalTags;
import com.tagnote.application.enrichment.model.ExternalTagInput;
import com.tagnote.application.enrichment.model.EnrichmentDeadline;
import com.tagnote.application.enrichment.model.ProviderEnrichmentResult;
import com.tagnote.application.enrichment.model.ProviderEnrichmentStatus;
import com.tagnote.application.enrichment.port.ExternalTagProvider;
import com.tagnote.application.enrichment.port.MusicBrainzCatalogClient;
import com.tagnote.domain.enrichment.assertion.EvidenceType;
import com.tagnote.domain.enrichment.observation.ExternalTagSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

@Order(100)
@Slf4j
@Component
@ConditionalOnProperty(prefix = "tag.enrichment.musicbrainz", name = "enabled", havingValue = "true")
public class MusicBrainzExternalTagProvider implements ExternalTagProvider {

    private final MusicBrainzCatalogClient client;
    private final MusicBrainzEntityMatchingService matchingService;
    private final ExternalEnrichmentProperties.EvidenceConfidence confidence;
    private final ExternalEnrichmentProperties.MusicBrainz config;
    private final MusicEntityNameNormalizer normalizer;
    private final MusicEditionTitleNormalizer editionTitleNormalizer;

    public MusicBrainzExternalTagProvider(
            MusicBrainzCatalogClient client,
            MusicBrainzEntityMatchingService matchingService,
            MusicEntityNameNormalizer normalizer,
            MusicEditionTitleNormalizer editionTitleNormalizer,
            ExternalEnrichmentProperties properties
    ) {
        this.client = client;
        this.matchingService = matchingService;
        this.normalizer = normalizer;
        this.editionTitleNormalizer = editionTitleNormalizer;
        this.config = properties.getMusicbrainz();
        this.confidence = properties.getEvidenceConfidence();
    }

    @Override
    public ExternalTagSource source() {
        return ExternalTagSource.MUSICBRAINZ;
    }

    @Override
    public ProviderEnrichmentResult collect(ImportedTrack track, EnrichmentDeadline deadline) {
        EssentialRequests requests = new EssentialRequests(deadline, track.getCatalogTrackId());
        String recordingId = track.getMusicBrainzRecordingId();
        if (recordingId == null) {
            RecordingCandidate matched = matchRecording(track, requests);
            recordingId = matched.id();
        }

        String acceptedRecordingId = recordingId;
        RecordingDetails recording = requests.execute(() -> client.getRecording(acceptedRecordingId));

        if (!recordingId.equals(recording.id())) {
            throw new ExternalProviderException(
                    ProviderEnrichmentStatus.NOT_FOUND,
                    "MusicBrainz Recording lookup returned a mismatched identity"
            );
        }
        List<ExternalTagInput> trackInputs = genreInputs(
                recording.genres(),
                "musicbrainz:recording:" + recordingId,
                confidence.getMusicbrainzRecordingGenre()
        );

        List<ReleaseGroupCandidate> releaseGroups = collectRecordingReleaseGroups(
                track, recordingId, deadline
        );
        String releaseGroupId = matchingService
                .matchReleaseGroup(track.getAlbum(), releaseGroups)
                .map(ReleaseGroupCandidate::id)
                .orElse(null);
        List<ExternalTagInput> albumInputs = List.of();
        if (releaseGroupId != null) {
            albumInputs = collectReleaseGroup(track, releaseGroupId, deadline);
        }

        return ProviderEnrichmentResult.completed(
                source(),
                new CollectedExternalTags(albumInputs, trackInputs),
                new CatalogExternalIdentityMatch(recordingId)
        );
    }

    @Override
    public ProviderEnrichmentResult collectAlbum(ImportedAlbum album, EnrichmentDeadline deadline) {
        List<String> artists = album.getArtists().stream().map(ImportedArtist::getName).toList();
        String canonicalTitle = editionTitleNormalizer.canonicalTitle(album.getTitle());
        List<ReleaseGroupCandidate> candidates = client.searchReleaseGroups(canonicalTitle, artists);
        List<ReleaseGroupCandidate> matches = matchingService.matchingReleaseGroups(
                album, canonicalTitle, candidates);
        String searchStage = "canonical";
        if (matches.isEmpty() && !canonicalTitle.equals(album.getTitle())) {
            if (!deadline.hasTimeFor(optionalRequestWorstCaseMs())) {
                throw new ExternalProviderException(ProviderEnrichmentStatus.TIMEOUT,
                        "MusicBrainz original Album search skipped due to insufficient remaining budget");
            }
            candidates = client.searchReleaseGroups(album.getTitle(), artists);
            matches = matchingService.matchingReleaseGroups(album, album.getTitle(), candidates);
            searchStage = "original";
        }
        if (matches.size() != 1) {
            throw new ExternalProviderException(ProviderEnrichmentStatus.NOT_FOUND,
                    "MusicBrainz Release Group was not uniquely matched. stage=" + searchStage
                            + ", candidateCount=" + candidates.size()
                            + ", matchedCount=" + matches.size()
                            + ", spotifyReleaseYear=" + album.getReleaseYear());
        }
        ReleaseGroupCandidate matched = matches.get(0);
        ReleaseGroupDetails details = client.getReleaseGroup(matched.id());
        if (!matched.id().equals(details.id())) {
            throw new ExternalProviderException(ProviderEnrichmentStatus.NOT_FOUND,
                    "MusicBrainz Release Group lookup returned a mismatched identity");
        }
        return ProviderEnrichmentResult.completed(source(),
                new CollectedExternalTags(genreInputs(details.genres(),
                        "musicbrainz:release-group:" + matched.id(),
                        confidence.getMusicbrainzReleaseGroupGenre()), List.of()),
                CatalogExternalIdentityMatch.none());
    }

    private List<ReleaseGroupCandidate> collectRecordingReleaseGroups(
            ImportedTrack track,
            String recordingId,
            EnrichmentDeadline deadline
    ) {
        if (!deadline.hasTimeFor(optionalRequestWorstCaseMs())) {
            logAlbumFailure(
                    track,
                    ProviderEnrichmentStatus.TIMEOUT,
                    deadline,
                    "MusicBrainz Recording releases lookup skipped due to insufficient remaining budget"
            );
            return List.of();
        }
        try {
            RecordingDetails releaseGroups = client.getRecordingReleaseGroups(recordingId);
            if (!recordingId.equals(releaseGroups.id())) {
                throw new ExternalProviderException(
                        ProviderEnrichmentStatus.NOT_FOUND,
                        "MusicBrainz Recording releases lookup returned a mismatched identity"
                );
            }
            return releaseGroups.releaseGroups();
        } catch (ExternalProviderException failure) {
            logAlbumFailure(track, failure.getStatus(), deadline, failure.getMessage());
            return List.of();
        }
    }

    private RecordingCandidate matchRecording(ImportedTrack track, EssentialRequests requests) {
        String isrc = normalizeIsrc(track.getIsrc());
        if (isrc != null) {
            List<RecordingCandidate> candidates = requests.execute(() -> client.searchByIsrc(isrc));
            RecordingCandidate matched = matchingService.matchRecordingByIsrc(track, candidates).orElse(null);
            if (matched != null) {
                return matched;
            }
        }
        List<RecordingCandidate> candidates = requests.execute(() -> client.searchByTitleAndArtists(
                track.getTitle(),
                track.getArtists().stream().map(ImportedArtist::getName).toList()
        ));
        RecordingCandidate matched = matchingService.matchRecordingByMetadata(track, candidates)
                .orElseThrow(() -> new ExternalProviderException(
                        ProviderEnrichmentStatus.NOT_FOUND,
                        "MusicBrainz Recording was not uniquely matched"
                ));
        return matched;
    }

    private List<ExternalTagInput> collectReleaseGroup(
            ImportedTrack track,
            String releaseGroupId,
            EnrichmentDeadline deadline
    ) {
        if (!deadline.hasTimeFor(optionalRequestWorstCaseMs())) {
            logAlbumFailure(
                    track,
                    ProviderEnrichmentStatus.TIMEOUT,
                    deadline,
                    "MusicBrainz Release Group lookup skipped due to insufficient remaining budget"
            );
            return List.of();
        }
        try {
            ReleaseGroupDetails releaseGroup = client.getReleaseGroup(releaseGroupId);
            if (!releaseGroupId.equals(releaseGroup.id())) {
                throw new ExternalProviderException(
                        ProviderEnrichmentStatus.NOT_FOUND,
                        "MusicBrainz Release Group lookup returned a mismatched identity"
                );
            }
            return genreInputs(
                    releaseGroup.genres(),
                    "musicbrainz:release-group:" + releaseGroupId,
                    confidence.getMusicbrainzReleaseGroupGenre()
            );
        } catch (ExternalProviderException releaseGroupFailure) {
            logAlbumFailure(
                    track,
                    releaseGroupFailure.getStatus(),
                    deadline,
                    releaseGroupFailure.getMessage()
            );
            return List.of();
        }
    }

    private void logAlbumFailure(
            ImportedTrack track,
            ProviderEnrichmentStatus status,
            EnrichmentDeadline deadline,
            String detail
    ) {
        log.warn(
                "External enrichment subject failed. provider={}, catalogTrackId={}, "
                        + "subject=ALBUM, status={}, remainingMs={}, detail={}",
                source(), track.getCatalogTrackId(), status, deadline.remainingMillis(), detail
        );
    }

    private long optionalRequestWorstCaseMs() {
        return config.getMinimumRequestIntervalMs()
                + config.getConnectTimeoutMs()
                + config.getReadTimeoutMs()
                + config.getCompletionSafetyMarginMs();
    }

    private List<ExternalTagInput> genreInputs(List<Genre> genres, String externalRef, double value) {
        LinkedHashMap<String, ExternalTagInput> unique = new LinkedHashMap<>();
        for (Genre genre : genres) {
            if (genre.name() == null || genre.name().isBlank() || genre.count() == null || genre.count() <= 0) {
                continue;
            }
            unique.putIfAbsent(normalizer.normalize(genre.name()), new ExternalTagInput(
                    source(), genre.name(), externalRef, EvidenceType.EXPLICIT_GENRE, value
            ));
        }
        return List.copyOf(unique.values());
    }

    private String normalizeIsrc(String isrc) {
        if (isrc == null || isrc.isBlank()) {
            return null;
        }
        String normalized = isrc.replaceAll("[^A-Za-z0-9]", "").toUpperCase(Locale.ROOT);
        return normalized.isBlank() ? null : normalized;
    }

    private final class EssentialRequests {

        private final EnrichmentDeadline deadline;
        private final Long catalogTrackId;
        private boolean retryAvailable = true;

        private EssentialRequests(EnrichmentDeadline deadline, Long catalogTrackId) {
            this.deadline = deadline;
            this.catalogTrackId = catalogTrackId;
        }

        private <T> T execute(Supplier<T> request) {
            try {
                return request.get();
            } catch (ExternalProviderException failure) {
                if (!retryAvailable
                        || !isRetryable(failure)
                        || Thread.currentThread().isInterrupted()
                        || !deadline.hasTimeFor(retryStartMinimumMs())) {
                    throw failure;
                }
                retryAvailable = false;
                log.warn(
                        "External enrichment request retrying. provider={}, catalogTrackId={}, "
                                + "reason={}, attempt=2, remainingMs={}",
                        source(), catalogTrackId, failure.getMessage(), deadline.remainingMillis()
                );
                return request.get();
            }
        }

        private boolean isRetryable(ExternalProviderException failure) {
            return failure.getStatus() == ProviderEnrichmentStatus.TIMEOUT || failure.isRetryable();
        }

        private long retryStartMinimumMs() {
            return config.getMinimumRequestIntervalMs() + config.getCompletionSafetyMarginMs();
        }
    }
}
