package com.tagnote.application.catalog.selection;

import com.tagnote.application.catalog.detail.TrackDetailReadService;
import com.tagnote.application.catalog.detail.model.TrackDetail;
import com.tagnote.application.catalog.importer.TrackImportService;
import com.tagnote.application.catalog.importer.CatalogExternalIdentityWriteService;
import com.tagnote.application.catalog.importer.model.ImportedTrack;
import com.tagnote.application.enrichment.ExternalEnrichmentCollector;
import com.tagnote.application.enrichment.ObservationProcessingService;
import com.tagnote.application.enrichment.model.CollectedExternalTags;
import com.tagnote.application.enrichment.model.ExternalEnrichmentCollection;
import com.tagnote.application.resolution.TagResolutionService;
import com.tagnote.application.resolution.model.ResolvedTagResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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

    public TrackDetail select(String spotifyTrackId) {
        ImportedTrack importedTrack = trackImportService.importTrack(spotifyTrackId);
        long catalogTrackId = importedTrack.getCatalogTrackId();
        TrackDetail resolvedDetail = trackDetailReadService.findResolved(importedTrack).orElse(null);
        if (resolvedDetail != null) {
            return resolvedDetail;
        }

        ExternalEnrichmentCollection enrichment = externalEnrichmentCollector.collect(importedTrack);
        catalogExternalIdentityWriteService.attach(catalogTrackId, enrichment.identityMatch());
        ImportedTrack enrichedTrack = importedTrack.withMusicBrainzIdentity(
                enrichment.identityMatch().musicBrainzRecordingId(),
                enrichment.identityMatch().musicBrainzReleaseGroupId()
        );
        CollectedExternalTags collected = enrichment.tags();
        if (!collected.albumInputs().isEmpty()) {
            observationProcessingService.processPersistedAlbum(enrichedTrack, collected.albumInputs());
            tagResolutionService.resolvePersistedAlbum(enrichedTrack);
        }
        if (!collected.trackInputs().isEmpty()) {
            observationProcessingService.processPersistedTrack(enrichedTrack, collected.trackInputs());
        }
        List<ResolvedTagResult> resolved = tagResolutionService.resolvePersistedTrack(enrichedTrack);

        return trackDetailReadService.fromResolved(enrichedTrack, resolved);
    }
}
