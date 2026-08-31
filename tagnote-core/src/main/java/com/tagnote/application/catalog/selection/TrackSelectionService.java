package com.tagnote.application.catalog.selection;

import com.tagnote.application.catalog.detail.TrackDetailReadService;
import com.tagnote.application.catalog.detail.model.TrackDetail;
import com.tagnote.application.catalog.importer.TrackImportService;
import com.tagnote.application.catalog.importer.model.ImportedTrack;
import com.tagnote.application.enrichment.ObservationProcessingService;
import com.tagnote.application.enrichment.model.CollectedExternalTags;
import com.tagnote.application.enrichment.model.ExternalTagInput;
import com.tagnote.application.enrichment.port.ExternalTagProvider;
import com.tagnote.application.resolution.TagResolutionService;
import com.tagnote.domain.enrichment.subject.SubjectRef;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TrackSelectionService {

    private final TrackImportService trackImportService;
    private final List<ExternalTagProvider> externalTagProviders;
    private final ObservationProcessingService observationProcessingService;
    private final TagResolutionService tagResolutionService;
    private final TrackDetailReadService trackDetailReadService;

    public TrackDetail select(String spotifyTrackId) {
        ImportedTrack importedTrack = trackImportService.importTrack(spotifyTrackId);
        long catalogTrackId = importedTrack.getCatalogTrackId();
        if (trackDetailReadService.hasResolvedProjection(catalogTrackId)) {
            return trackDetailReadService.getByCatalogTrackId(catalogTrackId);
        }

        CollectedExternalTags collected = collect(importedTrack);
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

    private CollectedExternalTags collect(ImportedTrack track) {
        List<ExternalTagInput> albumInputs = new ArrayList<>();
        List<ExternalTagInput> trackInputs = new ArrayList<>();
        for (ExternalTagProvider provider : externalTagProviders) {
            CollectedExternalTags collected = provider.collect(track);
            albumInputs.addAll(collected.albumInputs());
            trackInputs.addAll(collected.trackInputs());
        }
        return new CollectedExternalTags(albumInputs, trackInputs);
    }
}
