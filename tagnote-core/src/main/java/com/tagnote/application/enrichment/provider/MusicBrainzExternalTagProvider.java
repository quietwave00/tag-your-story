package com.tagnote.application.enrichment.provider;

import com.tagnote.application.catalog.importer.model.ImportedArtist;
import com.tagnote.application.catalog.importer.model.ImportedAlbum;
import com.tagnote.application.catalog.importer.model.ImportedTrack;
import com.tagnote.application.enrichment.config.ExternalEnrichmentProperties;
import com.tagnote.application.enrichment.diagnostics.EnrichmentDiagnostics;
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
import org.springframework.web.client.RestClientResponseException;

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
        genres("RECORDING", recordingId, recording.genres());

        List<ReleaseGroupCandidate> releaseGroups = collectRecordingReleaseGroups(
                track, recordingId, deadline
        );
        releaseGroups("RECORDING_LINKED", releaseGroups);
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
    public ProviderEnrichmentResult collectTrackDirect(ImportedTrack track, EnrichmentDeadline deadline) {
        EssentialRequests requests = new EssentialRequests(deadline, track.getCatalogTrackId());
        String recordingId = track.getMusicBrainzRecordingId();
        if (recordingId == null) recordingId = matchRecording(track, requests).id();
        String selectedId = recordingId;
        RecordingDetails recording = requests.execute(() -> client.getRecording(selectedId));
        if (!selectedId.equals(recording.id())) {
            throw new ExternalProviderException(ProviderEnrichmentStatus.NOT_FOUND,
                    "MusicBrainz Recording lookup returned a mismatched identity");
        }
        genres("RECORDING", selectedId, recording.genres());
        return ProviderEnrichmentResult.completed(source(),
                new CollectedExternalTags(List.of(), genreInputs(recording.genres(),
                        "musicbrainz:recording:" + selectedId,
                        confidence.getMusicbrainzRecordingGenre())),
                new CatalogExternalIdentityMatch(selectedId));
    }

    @Override
    public ProviderEnrichmentResult collectAlbumFromTrack(ImportedTrack track, EnrichmentDeadline deadline) {
        String recordingId = track.getMusicBrainzRecordingId();
        if (recordingId == null) return collectAlbum(track.getAlbum(), deadline);
        if (!deadline.hasTimeFor(optionalRequestWorstCaseMs())) {
            throw new ExternalProviderException(ProviderEnrichmentStatus.TIMEOUT,
                    "MusicBrainz Recording releases lookup skipped due to insufficient remaining budget");
        }
        RecordingDetails recording = client.getRecordingReleaseGroups(recordingId);
        if (!recordingId.equals(recording.id())) {
            throw new ExternalProviderException(ProviderEnrichmentStatus.NOT_FOUND,
                    "MusicBrainz Recording releases lookup returned a mismatched identity");
        }
        releaseGroups("RECORDING_LINKED", recording.releaseGroups());
        String releaseGroupId = matchingService.matchReleaseGroup(track.getAlbum(), recording.releaseGroups())
                .map(ReleaseGroupCandidate::id).orElse(null);
        if (releaseGroupId == null) {
            throw new ExternalProviderException(ProviderEnrichmentStatus.NOT_FOUND,
                    "MusicBrainz Release Group was not uniquely matched");
        }
        if (!deadline.hasTimeFor(optionalRequestWorstCaseMs())) {
            throw new ExternalProviderException(ProviderEnrichmentStatus.TIMEOUT,
                    "MusicBrainz Release Group lookup skipped due to insufficient remaining budget");
        }
        ReleaseGroupDetails group = client.getReleaseGroup(releaseGroupId);
        if (!releaseGroupId.equals(group.id())) {
            throw new ExternalProviderException(ProviderEnrichmentStatus.NOT_FOUND,
                    "MusicBrainz Release Group lookup returned a mismatched identity");
        }
        genres("RELEASE_GROUP", releaseGroupId, group.genres());
        return ProviderEnrichmentResult.completed(source(),
                new CollectedExternalTags(genreInputs(group.genres(),
                        "musicbrainz:release-group:" + releaseGroupId,
                        confidence.getMusicbrainzReleaseGroupGenre()), List.of()),
                CatalogExternalIdentityMatch.none());
    }

    @Override
    public ProviderEnrichmentResult collectAlbum(ImportedAlbum album, EnrichmentDeadline deadline) {
        List<String> artists = album.getArtists().stream().map(ImportedArtist::getName).toList();
        String canonicalTitle = editionTitleNormalizer.canonicalTitle(album.getTitle());
        List<ReleaseGroupCandidate> candidates = client.searchReleaseGroups(canonicalTitle, artists);
        List<ReleaseGroupCandidate> matches = matchingService.matchingReleaseGroups(
                album, canonicalTitle, candidates);
        releaseGroups("CANONICAL_SEARCH", candidates);
        EnrichmentDiagnostics.event("musicbrainz_release_group_match", java.util.Map.of(
                "stage", "CANONICAL_SEARCH", "matchedCount", matches.size()));
        String searchStage = "canonical";
        if (matches.isEmpty() && !canonicalTitle.equals(album.getTitle())) {
            if (!deadline.hasTimeFor(optionalRequestWorstCaseMs())) {
                throw new ExternalProviderException(ProviderEnrichmentStatus.TIMEOUT,
                        "MusicBrainz original Album search skipped due to insufficient remaining budget");
            }
            candidates = client.searchReleaseGroups(album.getTitle(), artists);
            matches = matchingService.matchingReleaseGroups(album, album.getTitle(), candidates);
            releaseGroups("ORIGINAL_SEARCH", candidates);
            EnrichmentDiagnostics.event("musicbrainz_release_group_match", java.util.Map.of(
                    "stage", "ORIGINAL_SEARCH", "matchedCount", matches.size()));
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
        genres("RELEASE_GROUP", matched.id(), details.genres());
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
        boolean isrcHttpNotFound = false;
        if (isrc != null) {
            try {
                List<RecordingCandidate> candidates = requests.execute(() -> client.searchByIsrc(isrc));
                RecordingCandidate matched = matchingService.matchRecordingByIsrc(track, candidates).orElse(null);
                recordings("ISRC", track, candidates, matched);
                if (matched != null) {
                    return matched;
                }
                EnrichmentDiagnostics.event("musicbrainz_metadata_fallback", java.util.Map.of(
                        "reason", candidates.isEmpty() ? "ISRC_EMPTY" : "ISRC_NO_VALID_MATCH"));
            } catch (ExternalProviderException failure) {
                if (!isHttp404(failure)) throw failure;
                isrcHttpNotFound = true;
                EnrichmentDiagnostics.event("musicbrainz_metadata_fallback", java.util.Map.of(
                        "reason", "ISRC_HTTP_404", "isrcHttpStatus", 404));
            }
        } else {
            EnrichmentDiagnostics.event("musicbrainz_metadata_fallback", java.util.Map.of("reason", "ISRC_ABSENT"));
        }
        List<RecordingCandidate> candidates = requests.execute(() -> client.searchByTitleAndArtists(
                track.getTitle(),
                track.getArtists().stream().map(ImportedArtist::getName).toList()
        ));
        RecordingCandidate matched = (isrcHttpNotFound
                ? matchingService.matchFirstValidRecordingAfterIsrcNotFound(track, candidates)
                : matchingService.matchRecordingByMetadata(track, candidates)).orElse(null);
        recordings("METADATA", track, candidates, matched);
        if (matched == null) throw new ExternalProviderException(
                ProviderEnrichmentStatus.NOT_FOUND, "MusicBrainz Recording was not uniquely matched");
        return matched;
    }

    private boolean isHttp404(ExternalProviderException failure) {
        return failure.getStatus() == ProviderEnrichmentStatus.NOT_FOUND
                && failure.getCause() instanceof RestClientResponseException response
                && response.getStatusCode().value() == 404;
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
            genres("RELEASE_GROUP", releaseGroupId, releaseGroup.genres());
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

    private void recordings(String stage, ImportedTrack track, List<RecordingCandidate> candidates,
                            RecordingCandidate selected) {
        if (!EnrichmentDiagnostics.enabled()) return;
        EnrichmentDiagnostics.event("musicbrainz_recording_candidates", java.util.Map.of(
                "stage", stage, "title", EnrichmentDiagnostics.safe(track.getTitle()),
                "artists", EnrichmentDiagnostics.bounded(track.getArtists().stream()
                        .map(ImportedArtist::getName).toList()),
                "isrc", stage.equals("ISRC") ? EnrichmentDiagnostics.safe(normalizeIsrc(track.getIsrc())) : "",
                "rawCount", candidates.size(),
                "selectedId", selected == null ? "" : selected.id(),
                "decision", selected == null ? candidates.isEmpty() ? "API_ZERO" : "NO_UNIQUE_MATCH" : "SELECTED",
                "candidates", EnrichmentDiagnostics.bounded(candidates.stream().map(candidate ->
                        java.util.Map.of("id", EnrichmentDiagnostics.safe(candidate.id()),
                                "title", EnrichmentDiagnostics.safe(candidate.title()),
                                "artists", EnrichmentDiagnostics.bounded(candidate.artistNames()),
                                "durationMs", candidate.durationMs() == null ? "UNKNOWN" : candidate.durationMs(),
                                "score", candidate.score() == null ? "UNKNOWN" : candidate.score())).toList())));
    }

    private void releaseGroups(String stage, List<ReleaseGroupCandidate> candidates) {
        if (!EnrichmentDiagnostics.enabled()) return;
        EnrichmentDiagnostics.event("musicbrainz_release_groups", java.util.Map.of(
                "stage", stage, "candidates", EnrichmentDiagnostics.bounded(candidates.stream().map(group ->
                        java.util.Map.of("id", EnrichmentDiagnostics.safe(group.id()),
                                "title", EnrichmentDiagnostics.safe(group.title()),
                                "year", group.releaseYear() == null ? "UNKNOWN" : group.releaseYear())).toList())));
    }

    private void genres(String subject, String id, List<Genre> genres) {
        if (!EnrichmentDiagnostics.enabled()) return;
        EnrichmentDiagnostics.event("musicbrainz_genres", java.util.Map.of(
                "subject", subject, "externalId", EnrichmentDiagnostics.safe(id),
                "genres", EnrichmentDiagnostics.bounded(genres.stream().map(genre ->
                        java.util.Map.of("name", EnrichmentDiagnostics.safe(genre.name()),
                                "count", genre.count() == null ? "UNKNOWN" : genre.count())).toList())));
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
