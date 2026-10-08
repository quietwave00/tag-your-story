package com.tagnote.application.catalog.selection;

import com.tagnote.application.catalog.detail.TrackDetailReadService;
import com.tagnote.application.catalog.detail.TagPreviewService;
import com.tagnote.application.catalog.detail.model.TrackDetail;
import com.tagnote.application.catalog.importer.TrackImportService;
import com.tagnote.application.catalog.importer.CatalogExternalIdentityWriteService;
import com.tagnote.application.catalog.importer.model.ImportedTrack;
import com.tagnote.application.enrichment.ExternalEnrichmentCollector;
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
import com.tagnote.domain.taxonomy.matching.TagNameNormalizer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;

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

    public TrackDetail select(String spotifyTrackId) {
        ImportedTrack importedTrack = trackImportService.importTrack(spotifyTrackId);
        long catalogTrackId = importedTrack.getCatalogTrackId();
        TrackDetail resolvedDetail = trackDetailReadService.findResolved(importedTrack).orElse(null);
        if (resolvedDetail != null) {
            return resolvedDetail;
        }
        if (completionService.completed(catalogTrackId)) {
            return new TrackDetail(importedTrack, List.of()).withPreview(
                    tagPreviewService.fromPersisted(catalogTrackId, importedTrack.getAlbum().getAlbumId()));
        }

        ExternalEnrichmentCollection enrichment = externalEnrichmentCollector.collect(importedTrack);
        catalogExternalIdentityWriteService.attach(catalogTrackId, enrichment.identityMatch());
        ImportedTrack enrichedTrack = importedTrack.withMusicBrainzRecordingId(
                enrichment.identityMatch().musicBrainzRecordingId());
        CollectedExternalTags collected = new CollectedExternalTags(
                enrichment.tags().albumInputs().stream().filter(this::persistable).toList(),
                enrichment.tags().trackInputs().stream().filter(this::persistable).toList());
        var matches = tagBootstrapService.prepare(collected);
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

        if (resolved.isEmpty() && enrichment.providerResults().stream().allMatch(result ->
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
        return detail.withPreview(tagPreviewService.fromProcessed(albumResult, trackResult));
    }

    private boolean persistable(ExternalTagInput input) {
        return input.rawName().length() <= 255
                && input.externalRef().length() <= 255
                && tagNameNormalizer.normalize(input.rawName()).value().length() <= 255;
    }
}
