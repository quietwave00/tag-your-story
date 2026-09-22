package com.tagnote.application.resolution;

import com.tagnote.application.catalog.importer.model.ImportedAlbum;
import com.tagnote.application.catalog.importer.model.ImportedTrack;
import com.tagnote.application.resolution.exception.ResolvedTagDuplicateException;
import com.tagnote.application.resolution.model.ResolvedTagResult;
import com.tagnote.domain.enrichment.subject.SubjectRef;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TagResolutionServiceTest {

    @Mock private TagResolutionWriteService writeService;
    @InjectMocks private TagResolutionService service;

    @Test
    void resolved_unique_충돌만_rollback_후_한번_재시도한다() {
        SubjectRef subject = SubjectRef.track(1L);
        List<ResolvedTagResult> expected = List.of();
        when(writeService.resolve(subject))
                .thenThrow(new ResolvedTagDuplicateException(new RuntimeException("duplicate")))
                .thenReturn(expected);

        assertThat(service.resolve(subject)).isSameAs(expected);
        verify(writeService, times(2)).resolve(subject);
    }

    @Test
    void 재시도도_실패하면_예외를_전파한다() {
        SubjectRef subject = SubjectRef.album(1L);
        doThrow(new ResolvedTagDuplicateException(new RuntimeException("duplicate")))
                .when(writeService).resolve(subject);

        assertThatThrownBy(() -> service.resolve(subject))
                .isInstanceOf(ResolvedTagDuplicateException.class);
        verify(writeService, times(2)).resolve(subject);
    }

    @Test
    void duplicate이_아닌_실패는_재시도하지_않는다() {
        SubjectRef subject = SubjectRef.track(1L);
        doThrow(new IllegalStateException("cycle")).when(writeService).resolve(subject);

        assertThatThrownBy(() -> service.resolve(subject)).isInstanceOf(IllegalStateException.class);
        verify(writeService).resolve(subject);
    }

    @Test
    void import에서_검증된_Track과_Album_ID로_subject_재조회없는_write를_호출한다() {
        ImportedTrack track = ImportedTrack.of(
                10L, "track-1", null, "Track", "ISRC", 1000, List.of(),
                ImportedAlbum.of(20L, "album-1", null, "Album", 2026, List.of())
        );
        List<ResolvedTagResult> expected = List.of();
        when(writeService.resolvePersistedAlbum(20L)).thenReturn(expected);
        when(writeService.resolvePersistedTrack(10L, 20L)).thenReturn(expected);

        assertThat(service.resolvePersistedAlbum(track)).isSameAs(expected);
        assertThat(service.resolvePersistedTrack(track)).isSameAs(expected);

        verify(writeService).resolvePersistedAlbum(20L);
        verify(writeService).resolvePersistedTrack(10L, 20L);
    }
}
