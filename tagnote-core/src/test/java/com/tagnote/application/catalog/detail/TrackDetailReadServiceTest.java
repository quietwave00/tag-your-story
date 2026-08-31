package com.tagnote.application.catalog.detail;

import com.tagnote.application.catalog.detail.model.TrackDetail;
import com.tagnote.application.catalog.importer.CatalogTrackReadService;
import com.tagnote.application.catalog.importer.model.ImportedAlbum;
import com.tagnote.application.catalog.importer.model.ImportedTrack;
import com.tagnote.domain.enrichment.subject.SubjectType;
import com.tagnote.domain.resolution.ResolvedStatus;
import com.tagnote.domain.resolution.SubjectTagResolvedEntity;
import com.tagnote.domain.taxonomy.tag.TagEntity;
import com.tagnote.infrastructure.persistence.resolution.SubjectTagResolvedJpaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrackDetailReadServiceTest {

    @Mock private CatalogTrackReadService catalogTrackReadService;
    @Mock private SubjectTagResolvedJpaRepository resolvedRepository;
    @InjectMocks private TrackDetailReadService service;

    @Test
    void HIDDEN을_포함한_projection_존재_여부를_별도로_판정한다() {
        when(resolvedRepository.existsBySubjectTypeAndSubjectId(SubjectType.TRACK, 10L))
                .thenReturn(true);

        assertThat(service.hasResolvedProjection(10L)).isTrue();
    }

    @Test
    void visible_resolved_projection을_system_tag_detail로_변환한다() {
        ImportedTrack imported = ImportedTrack.of(
                10L, "track-1", "Track", "ISRC", 180_000, List.of(),
                ImportedAlbum.of(20L, "album-1", "Album", 2026, List.of())
        );
        TagEntity tag = mock(TagEntity.class);
        SubjectTagResolvedEntity resolved = mock(SubjectTagResolvedEntity.class);
        when(tag.getTagId()).thenReturn(3L);
        when(tag.getName()).thenReturn("Ambient");
        when(resolved.getTag()).thenReturn(tag);
        when(resolved.getScore()).thenReturn(0.9);
        when(catalogTrackReadService.getByCatalogId(10L)).thenReturn(imported);
        when(resolvedRepository.findVisibleBySubjectWithTag(
                SubjectType.TRACK, 10L, ResolvedStatus.HIDDEN
        )).thenReturn(List.of(resolved));

        TrackDetail detail = service.getByCatalogTrackId(10L);

        assertThat(detail.track()).isSameAs(imported);
        assertThat(detail.systemTags()).singleElement().satisfies(systemTag -> {
            assertThat(systemTag.tagId()).isEqualTo(3L);
            assertThat(systemTag.name()).isEqualTo("Ambient");
            assertThat(systemTag.score()).isEqualTo(0.9);
        });
        verify(resolvedRepository).findVisibleBySubjectWithTag(
                SubjectType.TRACK, 10L, ResolvedStatus.HIDDEN
        );
    }
}
