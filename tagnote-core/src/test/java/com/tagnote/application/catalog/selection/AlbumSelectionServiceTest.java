package com.tagnote.application.catalog.selection;

import com.tagnote.application.catalog.detail.AlbumDetailReadService;
import com.tagnote.application.catalog.detail.TagPreviewService;
import com.tagnote.application.catalog.detail.model.AlbumDetail;
import com.tagnote.application.catalog.detail.model.SystemTagDetail;
import com.tagnote.application.catalog.importer.AlbumImportService;
import com.tagnote.application.catalog.importer.model.ImportedAlbum;
import com.tagnote.application.enrichment.ExternalEnrichmentCollector;
import com.tagnote.application.enrichment.AlbumTagCompletionService;
import com.tagnote.application.enrichment.ObservationProcessingService;
import com.tagnote.application.enrichment.TagBootstrapService;
import com.tagnote.application.enrichment.model.CatalogExternalIdentityMatch;
import com.tagnote.application.enrichment.model.CollectedExternalTags;
import com.tagnote.application.enrichment.model.ExternalEnrichmentCollection;
import com.tagnote.application.enrichment.model.ExternalTagInput;
import com.tagnote.application.enrichment.model.ObservationProcessingResult;
import com.tagnote.application.resolution.TagResolutionService;
import com.tagnote.application.resolution.model.ResolvedTagResult;
import com.tagnote.domain.enrichment.assertion.EvidenceType;
import com.tagnote.domain.enrichment.observation.ExternalTagSource;
import com.tagnote.domain.enrichment.subject.SubjectRef;
import com.tagnote.domain.taxonomy.matching.TagNameNormalizer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlbumSelectionServiceTest {
    @Mock AlbumImportService importer;
    @Mock ExternalEnrichmentCollector collector;
    @Mock TagBootstrapService bootstrap;
    @Mock ObservationProcessingService observations;
    @Mock TagResolutionService resolver;
    @Mock AlbumDetailReadService details;
    @Mock TagPreviewService previews;
    @Mock AlbumTagCompletionService completion;
    AlbumSelectionService service;

    @BeforeEach
    void setUp() {
        service = new AlbumSelectionService(importer, collector, bootstrap, observations,
                resolver, details, previews, completion, new TagNameNormalizer());
    }

    @Test
    void 기존_album_resolved는_외부_호출없이_반환한다() {
        ImportedAlbum album = album();
        AlbumDetail detail = new AlbumDetail(album, List.of(new SystemTagDetail(1L, "Rock", 0.9)));
        when(importer.importAlbum("album-1")).thenReturn(album);
        when(details.findResolved(album)).thenReturn(Optional.of(detail));

        assertThat(service.select("album-1")).isSameAs(detail);
        verify(collector, never()).collectAlbum(any());
    }

    @Test
    void 완료된_빈_album은_저장된_preview를_재사용한다() {
        ImportedAlbum album = album();
        when(importer.importAlbum("album-1")).thenReturn(album);
        when(details.findResolved(album)).thenReturn(Optional.empty());
        when(completion.completed(5L)).thenReturn(true);

        assertThat(service.select("album-1").systemTags()).isEmpty();
        verify(previews).fromPersistedAlbum(5L);
        verify(collector, never()).collectAlbum(any());
    }

    @Test
    void 신규_album은_album_input만_처리하고_album을_resolve한다() {
        ImportedAlbum album = album();
        ExternalTagInput input = new ExternalTagInput(ExternalTagSource.DISCOGS,
                "Rock", "discogs:release:1", EvidenceType.EXPLICIT_GENRE, 0.9);
        AlbumDetail detail = new AlbumDetail(album, List.of());
        when(importer.importAlbum("album-1")).thenReturn(album);
        when(details.findResolved(album)).thenReturn(Optional.empty());
        when(collector.collectAlbum(album)).thenReturn(new ExternalEnrichmentCollection(
                new CollectedExternalTags(List.of(input), List.of()),
                CatalogExternalIdentityMatch.none(), List.of()));
        when(bootstrap.prepare(new CollectedExternalTags(List.of(input), List.of()))).thenReturn(Map.of());
        when(observations.processPersistedAlbum(album, List.of(input), Map.of()))
                .thenReturn(ObservationProcessingResult.empty());
        when(resolver.resolve(SubjectRef.album(5L))).thenReturn(List.<ResolvedTagResult>of());
        when(details.fromResolved(album, List.of())).thenReturn(detail);

        assertThat(service.select("album-1")).isEqualTo(detail);
        verify(observations).processPersistedAlbum(album, List.of(input), Map.of());
        verify(resolver).resolve(SubjectRef.album(5L));
    }

    private ImportedAlbum album() {
        return ImportedAlbum.of(5L, "album-1", "Album", 2024, List.of());
    }
}
