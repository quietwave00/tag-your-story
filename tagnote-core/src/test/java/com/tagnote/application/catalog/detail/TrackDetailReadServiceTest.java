package com.tagnote.application.catalog.detail;

import com.tagnote.application.catalog.detail.model.TrackDetail;
import com.tagnote.application.catalog.importer.model.ImportedAlbum;
import com.tagnote.application.catalog.importer.model.ImportedTrack;
import com.tagnote.application.resolution.model.ResolvedTagResult;
import com.tagnote.domain.enrichment.subject.SubjectType;
import com.tagnote.domain.resolution.ResolutionReason;
import com.tagnote.domain.resolution.ResolvedStatus;
import com.tagnote.domain.resolution.SubjectTagResolvedEntity;
import com.tagnote.domain.taxonomy.tag.TagEntity;
import com.tagnote.infrastructure.persistence.resolution.SubjectTagResolvedJpaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrackDetailReadServiceTest {

    @Mock private SubjectTagResolvedJpaRepository resolvedRepository;
    @InjectMocks private TrackDetailReadService service;

    @Test
    void resolved_fast_path는_projection을_한번_읽고_HIDDEN을_제외한다() {
        ImportedTrack imported = importedTrack();
        TagEntity visibleTag = mock(TagEntity.class);
        SubjectTagResolvedEntity visible = mock(SubjectTagResolvedEntity.class);
        SubjectTagResolvedEntity hidden = mock(SubjectTagResolvedEntity.class);
        when(visibleTag.getTagId()).thenReturn(3L);
        when(visibleTag.getName()).thenReturn("Ambient");
        when(visible.getTag()).thenReturn(visibleTag);
        when(visible.getScore()).thenReturn(0.9);
        when(visible.getStatus()).thenReturn(ResolvedStatus.ACTIVE);
        when(hidden.getStatus()).thenReturn(ResolvedStatus.HIDDEN);
        when(resolvedRepository.findAllBySubjectWithTag(SubjectType.TRACK, 10L))
                .thenReturn(List.of(hidden, visible));

        TrackDetail detail = service.findResolved(imported).orElseThrow();

        assertThat(detail.track()).isSameAs(imported);
        assertThat(detail.systemTags()).singleElement()
                .extracting(systemTag -> systemTag.name())
                .isEqualTo("Ambient");
    }

    @Test
    void first_load_detail은_이미_계산한_resolved_result를_재조회없이_사용한다() {
        ImportedTrack imported = importedTrack();
        ResolvedTagResult visible = new ResolvedTagResult(
                3L, "Ambient", 0.9, ResolvedStatus.ACTIVE,
                ResolutionReason.AUTO, LocalDateTime.now()
        );
        ResolvedTagResult hidden = new ResolvedTagResult(
                4L, "Hidden", 0.8, ResolvedStatus.HIDDEN,
                ResolutionReason.AUTO, LocalDateTime.now()
        );

        TrackDetail detail = service.fromResolved(imported, List.of(visible, hidden));

        assertThat(detail.track()).isSameAs(imported);
        assertThat(detail.systemTags()).extracting(systemTag -> systemTag.name())
                .containsExactly("Ambient");
        verify(resolvedRepository, never()).findVisibleBySubjectWithTag(
                SubjectType.TRACK, 10L, ResolvedStatus.HIDDEN
        );
    }

    private ImportedTrack importedTrack() {
        return ImportedTrack.of(
                10L, "track-1", null, "Track", "ISRC", 180_000, List.of(),
                ImportedAlbum.of(20L, "album-1", null, "Album", 2026, List.of())
        );
    }
}
