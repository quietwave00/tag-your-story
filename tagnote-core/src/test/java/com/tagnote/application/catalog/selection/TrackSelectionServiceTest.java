package com.tagnote.application.catalog.selection;

import com.tagnote.application.catalog.detail.TrackDetailReadService;
import com.tagnote.application.catalog.detail.model.TrackDetail;
import com.tagnote.application.catalog.importer.TrackImportService;
import com.tagnote.application.catalog.importer.model.ImportedAlbum;
import com.tagnote.application.catalog.importer.model.ImportedTrack;
import com.tagnote.application.enrichment.ObservationProcessingService;
import com.tagnote.application.enrichment.model.CollectedExternalTags;
import com.tagnote.application.enrichment.model.ExternalTagInput;
import com.tagnote.application.enrichment.port.ExternalTagProvider;
import com.tagnote.application.resolution.TagResolutionService;
import com.tagnote.domain.enrichment.assertion.EvidenceType;
import com.tagnote.domain.enrichment.observation.ExternalTagSource;
import com.tagnote.domain.enrichment.subject.SubjectRef;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrackSelectionServiceTest {

    @Mock private TrackImportService trackImportService;
    @Mock private ExternalTagProvider externalTagProvider;
    @Mock private ObservationProcessingService observationProcessingService;
    @Mock private TagResolutionService tagResolutionService;
    @Mock private TrackDetailReadService trackDetailReadService;

    private TrackSelectionService service;

    @BeforeEach
    void setUp() {
        service = new TrackSelectionService(
                trackImportService,
                List.of(externalTagProvider),
                observationProcessingService,
                tagResolutionService,
                trackDetailReadService
        );
    }

    @Test
    void resolved_projection이_있으면_provider와_pipeline을_호출하지_않는다() {
        ImportedTrack imported = importedTrack();
        TrackDetail detail = new TrackDetail(imported, List.of());
        when(trackImportService.importTrack("track-1")).thenReturn(imported);
        when(trackDetailReadService.hasResolvedProjection(10L)).thenReturn(true);
        when(trackDetailReadService.getByCatalogTrackId(10L)).thenReturn(detail);

        assertThat(service.select("track-1")).isSameAs(detail);

        verify(externalTagProvider, never()).collect(imported);
        verify(observationProcessingService, never())
                .process(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyList());
        verify(tagResolutionService, never()).resolve(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void album_evidence를_먼저_처리하고_track을_resolve한_뒤_projection을_재조회한다() {
        ImportedTrack imported = importedTrack();
        ExternalTagInput albumInput = input("Album Genre", "release:album-1");
        ExternalTagInput trackInput = input("Track Genre", "recording:track-1");
        TrackDetail detail = new TrackDetail(imported, List.of());
        when(trackImportService.importTrack("track-1")).thenReturn(imported);
        when(externalTagProvider.collect(imported)).thenReturn(
                new CollectedExternalTags(List.of(albumInput), List.of(trackInput))
        );
        when(trackDetailReadService.getByCatalogTrackId(10L)).thenReturn(detail);

        assertThat(service.select("track-1")).isSameAs(detail);

        InOrder order = inOrder(
                trackImportService,
                externalTagProvider,
                observationProcessingService,
                tagResolutionService,
                trackDetailReadService
        );
        order.verify(trackImportService).importTrack("track-1");
        order.verify(externalTagProvider).collect(imported);
        order.verify(observationProcessingService).process(SubjectRef.album(20L), List.of(albumInput));
        order.verify(tagResolutionService).resolve(SubjectRef.album(20L));
        order.verify(observationProcessingService).process(SubjectRef.track(10L), List.of(trackInput));
        order.verify(tagResolutionService).resolve(SubjectRef.track(10L));
        order.verify(trackDetailReadService).getByCatalogTrackId(10L);
    }

    @Test
    void direct_input과_provider가_없어도_album_inheritance를_위해_track_resolve를_실행한다() {
        ImportedTrack imported = importedTrack();
        TrackDetail detail = new TrackDetail(imported, List.of());
        service = new TrackSelectionService(
                trackImportService,
                List.of(),
                observationProcessingService,
                tagResolutionService,
                trackDetailReadService
        );
        when(trackImportService.importTrack("track-1")).thenReturn(imported);
        when(trackDetailReadService.getByCatalogTrackId(10L)).thenReturn(detail);

        assertThat(service.select("track-1")).isSameAs(detail);

        verify(observationProcessingService, never())
                .process(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyList());
        verify(tagResolutionService).resolve(SubjectRef.track(10L));
    }

    private ExternalTagInput input(String name, String ref) {
        return new ExternalTagInput(
                ExternalTagSource.MUSICBRAINZ,
                name,
                ref,
                EvidenceType.EXPLICIT_GENRE,
                0.9
        );
    }

    private ImportedTrack importedTrack() {
        return ImportedTrack.of(
                10L,
                "track-1",
                "Track",
                "ISRC",
                180_000,
                List.of(),
                ImportedAlbum.of(20L, "album-1", "Album", 2026, List.of())
        );
    }
}
