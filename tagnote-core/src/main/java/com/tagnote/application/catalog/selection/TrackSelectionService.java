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
import com.tagnote.domain.enrichment.subject.SubjectRef;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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
        if (trackDetailReadService.hasResolvedProjection(catalogTrackId)) {
            return trackDetailReadService.getByCatalogTrackId(catalogTrackId);
        }

        ExternalEnrichmentCollection enrichment = externalEnrichmentCollector.collect(importedTrack);
        catalogExternalIdentityWriteService.attach(catalogTrackId, enrichment.identityMatch());
        CollectedExternalTags collected = enrichment.tags();
        long albumId = importedTrack.getAlbum().getAlbumId();
        if (!collected.albumInputs().isEmpty()) {
            observationProcessingService.process(
                    SubjectRef.album(albumId), collected.albumInputs()
            );
            tagResolutionService.resolve(SubjectRef.album(albumId));
        }
        if (!collected.trackInputs().isEmpty()) {
            observationProcessingService.process(
                    SubjectRef.track(catalogTrackId), collected.trackInputs()
            );
        }
        tagResolutionService.resolve(SubjectRef.track(catalogTrackId));

        return trackDetailReadService.getByCatalogTrackId(catalogTrackId);
    }
}
