package com.tagnote.application.catalog.selection;

import com.tagnote.application.catalog.detail.TrackDetailReadService;
import com.tagnote.application.catalog.detail.CatalogDetailService;
import com.tagnote.application.catalog.detail.TagPreviewService;
import com.tagnote.application.catalog.detail.model.TrackDetail;
import com.tagnote.application.catalog.importer.TrackImportService;
import com.tagnote.application.catalog.importer.CatalogExternalIdentityWriteService;
import com.tagnote.application.catalog.importer.model.ImportedTrack;
import com.tagnote.application.enrichment.ExternalEnrichmentCollector;
import com.tagnote.application.enrichment.AlbumInheritanceRefreshService;
import com.tagnote.application.enrichment.diagnostics.EnrichmentDiagnostics;
import com.tagnote.application.enrichment.ObservationProcessingService;
import com.tagnote.application.enrichment.TagBootstrapService;
import com.tagnote.application.enrichment.TrackTagCompletionService;
import com.tagnote.application.enrichment.model.CollectedExternalTags;
import com.tagnote.application.enrichment.model.ExternalEnrichmentCollection;
import com.tagnote.application.enrichment.model.ExternalTagInput;
import com.tagnote.application.enrichment.model.ObservationProcessingResult;
import com.tagnote.application.enrichment.model.ProviderEnrichmentStatus;
import com.tagnote.application.resolution.TagResolutionService;
import com.tagnote.application.resolution.model.ResolvedTagResult;
import com.tagnote.domain.resolution.ResolutionReason;
import com.tagnote.domain.taxonomy.matching.TagNameNormalizer;
import com.tagnote.domain.taxonomy.matching.TagMatchResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TrackSelectionService {

    private final TrackImportService trackImportService;
    private final ExternalEnrichmentCollector externalEnrichmentCollector;
    private final CatalogExternalIdentityWriteService catalogExternalIdentityWriteService;
    private final ObservationProcessingService observationProcessingService;
    private final TagResolutionService tagResolutionService;
    private final TrackDetailReadService trackDetailReadService;
    private final TagBootstrapService tagBootstrapService;
    private final TagPreviewService tagPreviewService;
    private final TrackTagCompletionService completionService;
    private final TagNameNormalizer tagNameNormalizer;
    private final CatalogDetailService catalogDetails;
    private final AlbumInheritanceRefreshService albumInheritanceRefreshService;

    public TrackDetail select(String spotifyTrackId) {
        try (EnrichmentDiagnostics.Scope ignored = EnrichmentDiagnostics.open("IMPORT_PIPELINE", "TRACK")) {
        ImportedTrack importedTrack = trackImportService.importTrack(spotifyTrackId);
        EnrichmentDiagnostics.started(importedTrack);
        EnrichmentDiagnostics.release(importedTrack.getAlbum(), catalogDetails);
        long catalogTrackId = importedTrack.getCatalogTrackId();
        TrackDetail resolvedDetail = trackDetailReadService.findResolved(importedTrack).orElse(null);
        if (resolvedDetail != null) {
            EnrichmentDiagnostics.event("import_skipped", Map.of("reason", "ALREADY_RESOLVED"));
            return resolvedDetail;
        }
        if (completionService.completed(catalogTrackId)) {
            EnrichmentDiagnostics.event("import_skipped", Map.of("reason", "EMPTY_COMPLETED"));
            return new TrackDetail(importedTrack, List.of()).withPreview(
                    tagPreviewService.fromPersisted(catalogTrackId, importedTrack.getAlbum().getAlbumId()));
        }

        ExternalEnrichmentCollection enrichment = externalEnrichmentCollector.collect(importedTrack);
        boolean albumEvidenceOwned = enrichment.albumEvidence().isEmpty()
                || externalEnrichmentCollector.ownsAlbumEvidence(importedTrack.getAlbum().getAlbumId(), enrichment);
        catalogExternalIdentityWriteService.attach(catalogTrackId, enrichment.identityMatch());
        ImportedTrack enrichedTrack = importedTrack.withMusicBrainzRecordingId(
                enrichment.identityMatch().musicBrainzRecordingId());
        CollectedExternalTags collected = new CollectedExternalTags(
                (enrichment.albumEvidence().isEmpty() ? enrichment.tags().albumInputs()
                        : externalEnrichmentCollector.usableAlbumInputs(
                                importedTrack.getAlbum().getAlbumId(), enrichment))
                        .stream().filter(this::persistable).toList(),
                enrichment.tags().trackInputs().stream().filter(this::persistable).toList());
        Map<String, TagMatchResult> matches = tagBootstrapService.prepare(collected);
        ObservationProcessingResult albumResult = ObservationProcessingResult.empty();
        ObservationProcessingResult trackResult = ObservationProcessingResult.empty();
        if (!collected.albumInputs().isEmpty()) {
            albumResult = observationProcessingService.processPersistedAlbum(
                    enrichedTrack, collected.albumInputs(), matches);
            tagResolutionService.resolvePersistedAlbum(enrichedTrack);
        }
        if (!collected.trackInputs().isEmpty()) {
            trackResult = observationProcessingService.processPersistedTrack(
                    enrichedTrack, collected.trackInputs(), matches);
        }
        List<ResolvedTagResult> resolved = tagResolutionService.resolvePersistedTrack(enrichedTrack);
        if (albumResult.createdAssertionCount() > 0) {
            albumInheritanceRefreshService.refresh(importedTrack.getAlbum().getAlbumId(), catalogTrackId);
        }
        boolean albumEvidenceCommitted = enrichment.albumEvidence().isEmpty()
                || externalEnrichmentCollector.completeAlbumEvidence(importedTrack.getAlbum().getAlbumId(), enrichment,
                        collected.albumInputs());
        albumEvidenceOwned = albumEvidenceOwned && albumEvidenceCommitted;
        if (EnrichmentDiagnostics.enabled()) EnrichmentDiagnostics.event("tag_calculation_completed", Map.of(
                "bootstrapMatched", matches.values().stream().filter(match ->
                        match.getStatus() == TagMatchResult.Status.MATCHED).count(),
                "albumObservationsNew", albumResult.newObservationCount(),
                "albumObservationsMatched", albumResult.matchedObservationCount(),
                "trackObservationsNew", trackResult.newObservationCount(),
                "trackObservationsMatched", trackResult.matchedObservationCount(),
                "assertionsCreated", albumResult.createdAssertionCount() + trackResult.createdAssertionCount(),
                "assertionsReused", albumResult.reusedAssertionCount() + trackResult.reusedAssertionCount(),
                "resolvedDirectCount", resolved.stream().filter(tag ->
                        tag.reason() != ResolutionReason.INHERITED_FROM_ALBUM).count(),
                "resolvedInheritedCount", resolved.stream().filter(tag ->
                        tag.reason() == ResolutionReason.INHERITED_FROM_ALBUM).count(),
                "resolved", EnrichmentDiagnostics.bounded(resolved.stream().map(tag -> Map.of(
                        "tagId", tag.tagId(), "name", tag.tagName(), "score", tag.score(),
                        "reason", tag.reason().name(), "status", tag.status().name())).toList())));

        if (resolved.isEmpty() && albumEvidenceOwned && !enrichment.providerResults().isEmpty()
                && enrichment.providerResults().stream().allMatch(result ->
                result.status() == ProviderEnrichmentStatus.SUCCESS
                        || result.status() == ProviderEnrichmentStatus.EMPTY
                        || result.status() == ProviderEnrichmentStatus.NOT_FOUND)) {
            try {
                completionService.complete(catalogTrackId);
            } catch (DataIntegrityViolationException conflict) {
                if (!completionService.completed(catalogTrackId)) throw conflict;
            }
        }

        TrackDetail detail = trackDetailReadService.fromResolved(enrichedTrack, resolved);
        if (!resolved.isEmpty()) return detail;
        TrackDetail preview = detail.withPreview(tagPreviewService.fromProcessed(albumResult, trackResult));
        EnrichmentDiagnostics.event("preview_completed", Map.of("count", preview.previewTags().size()));
        return preview;
        }
    }

    private boolean persistable(ExternalTagInput input) {
        return input.rawName().length() <= 255
                && input.externalRef().length() <= 255
                && tagNameNormalizer.normalize(input.rawName()).value().length() <= 255;
    }
}
