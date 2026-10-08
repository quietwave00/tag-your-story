package com.tagnote.application.catalog.selection;

import com.tagnote.application.catalog.detail.TrackDetailReadService;
import com.tagnote.application.catalog.detail.TagPreviewService;
import com.tagnote.application.catalog.detail.model.TrackDetail;
import com.tagnote.application.catalog.importer.CatalogExternalIdentityWriteService;
import com.tagnote.application.catalog.importer.TrackImportService;
import com.tagnote.application.catalog.importer.model.ImportedAlbum;
import com.tagnote.application.catalog.importer.model.ImportedTrack;
import com.tagnote.application.enrichment.ExternalEnrichmentCollector;
import com.tagnote.application.enrichment.ObservationProcessingService;
import com.tagnote.application.enrichment.TagBootstrapService;
import com.tagnote.application.enrichment.TrackTagCompletionService;
import com.tagnote.application.enrichment.model.CatalogExternalIdentityMatch;
import com.tagnote.application.enrichment.model.CollectedExternalTags;
import com.tagnote.application.enrichment.model.ExternalEnrichmentCollection;
import com.tagnote.application.enrichment.model.ExternalTagInput;
import com.tagnote.application.enrichment.model.ObservationProcessingResult;
import com.tagnote.application.resolution.TagResolutionService;
import com.tagnote.domain.enrichment.assertion.EvidenceType;
import com.tagnote.domain.enrichment.observation.ExternalTagSource;
import com.tagnote.domain.taxonomy.matching.TagNameNormalizer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrackSelectionServiceTest {

    @Mock private TrackImportService trackImportService;
    @Mock private ExternalEnrichmentCollector externalEnrichmentCollector;
    @Mock private CatalogExternalIdentityWriteService catalogExternalIdentityWriteService;
    @Mock private ObservationProcessingService observationProcessingService;
    @Mock private TagResolutionService tagResolutionService;
    @Mock private TrackDetailReadService trackDetailReadService;
    @Mock private TagBootstrapService tagBootstrapService;
    @Mock private TagPreviewService tagPreviewService;
    @Mock private TrackTagCompletionService completionService;

    private TrackSelectionService service;

    @BeforeEach
    void setUp() {
        service = new TrackSelectionService(
                trackImportService,
                externalEnrichmentCollector,
                catalogExternalIdentityWriteService,
                observationProcessingService,
                tagResolutionService,
                trackDetailReadService,
                tagBootstrapService,
                tagPreviewService,
                completionService,
                new TagNameNormalizer()
        );
    }

    @Test
    void resolved_projection이_있으면_provider와_pipeline을_호출하지_않는다() {
        ImportedTrack imported = importedTrack();
        TrackDetail detail = new TrackDetail(imported, List.of());
        when(trackImportService.importTrack("track-1")).thenReturn(imported);
        when(trackDetailReadService.findResolved(imported)).thenReturn(Optional.of(detail));

        assertThat(service.select("track-1")).isSameAs(detail);

        verify(externalEnrichmentCollector, never()).collect(imported);
        verify(catalogExternalIdentityWriteService, never())
                .attach(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.any());
        verify(observationProcessingService, never())
                .processPersistedTrack(
                        org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyList()
                );
        verify(tagResolutionService, never()).resolvePersistedTrack(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void album_evidence를_먼저_처리하고_track_resolve_결과로_detail을_조립한다() {
        ImportedTrack imported = importedTrack();
        ExternalTagInput albumInput = input("Album Genre", "release:album-1");
        ExternalTagInput trackInput = input("Track Genre", "recording:track-1");
        TrackDetail detail = new TrackDetail(imported, List.of());
        when(trackImportService.importTrack("track-1")).thenReturn(imported);
        when(trackDetailReadService.findResolved(imported)).thenReturn(Optional.empty());
        when(externalEnrichmentCollector.collect(imported)).thenReturn(
                new ExternalEnrichmentCollection(
                        new CollectedExternalTags(List.of(albumInput), List.of(trackInput)),
                        CatalogExternalIdentityMatch.none(),
                        List.of()
                )
        );
        when(tagResolutionService.resolvePersistedTrack(imported)).thenReturn(List.of());
        when(trackDetailReadService.fromResolved(imported, List.of())).thenReturn(detail);
        when(tagBootstrapService.prepare(org.mockito.ArgumentMatchers.any())).thenReturn(Map.of());
        when(observationProcessingService.processPersistedAlbum(imported, List.of(albumInput), Map.of()))
                .thenReturn(ObservationProcessingResult.empty());
        when(observationProcessingService.processPersistedTrack(imported, List.of(trackInput), Map.of()))
                .thenReturn(ObservationProcessingResult.empty());
        when(tagPreviewService.fromProcessed(ObservationProcessingResult.empty(),
                ObservationProcessingResult.empty()))
                .thenReturn(List.of());

        assertThat(service.select("track-1")).isSameAs(detail);

        InOrder order = inOrder(
                trackImportService,
                externalEnrichmentCollector,
                catalogExternalIdentityWriteService,
                observationProcessingService,
                tagResolutionService,
                trackDetailReadService
        );
        order.verify(trackImportService).importTrack("track-1");
        order.verify(externalEnrichmentCollector).collect(imported);
        order.verify(catalogExternalIdentityWriteService).attach(10L, CatalogExternalIdentityMatch.none());
        order.verify(observationProcessingService).processPersistedAlbum(imported, List.of(albumInput), Map.of());
        order.verify(tagResolutionService).resolvePersistedAlbum(imported);
        order.verify(observationProcessingService).processPersistedTrack(imported, List.of(trackInput), Map.of());
        order.verify(tagResolutionService).resolvePersistedTrack(imported);
        order.verify(trackDetailReadService).fromResolved(imported, List.of());
    }

    @Test
    void direct_input과_provider가_없어도_album_inheritance를_위해_track_resolve를_실행한다() {
        ImportedTrack imported = importedTrack();
        TrackDetail detail = new TrackDetail(imported, List.of());
        service = new TrackSelectionService(
                trackImportService,
                externalEnrichmentCollector,
                catalogExternalIdentityWriteService,
                observationProcessingService,
                tagResolutionService,
                trackDetailReadService,
                tagBootstrapService,
                tagPreviewService,
                completionService,
                new TagNameNormalizer()
        );
        when(trackImportService.importTrack("track-1")).thenReturn(imported);
        when(trackDetailReadService.findResolved(imported)).thenReturn(Optional.empty());
        when(externalEnrichmentCollector.collect(imported)).thenReturn(ExternalEnrichmentCollection.empty());
        when(tagResolutionService.resolvePersistedTrack(imported)).thenReturn(List.of());
        when(trackDetailReadService.fromResolved(imported, List.of())).thenReturn(detail);
        when(tagBootstrapService.prepare(org.mockito.ArgumentMatchers.any())).thenReturn(Map.of());
        when(tagPreviewService.fromProcessed(ObservationProcessingResult.empty(),
                ObservationProcessingResult.empty()))
                .thenReturn(List.of());

        assertThat(service.select("track-1")).isSameAs(detail);

        verify(observationProcessingService, never())
                .processPersistedTrack(
                        org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyList()
                );
        verify(tagResolutionService).resolvePersistedTrack(imported);
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
                null,
                "Track",
                "ISRC",
                180_000,
                List.of(),
                ImportedAlbum.of(20L, "album-1", "Album", 2026, List.of())
        );
    }
}
