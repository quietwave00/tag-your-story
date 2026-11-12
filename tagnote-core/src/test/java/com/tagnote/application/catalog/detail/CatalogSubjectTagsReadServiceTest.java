package com.tagnote.application.catalog.detail;

import com.tagnote.application.catalog.detail.model.PreviewTagDetail;
import com.tagnote.application.catalog.detail.model.SubjectTags;
import com.tagnote.application.catalog.detail.model.SystemTagDetail;
import com.tagnote.application.catalog.detail.model.TagDisplayStatus;
import com.tagnote.core.exception.CustomException;
import com.tagnote.core.exception.ExceptionCode;
import com.tagnote.domain.catalog.album.AlbumEntity;
import com.tagnote.domain.catalog.track.TrackEntity;
import com.tagnote.domain.enrichment.observation.ExternalTagSource;
import com.tagnote.infrastructure.persistence.catalog.AlbumJpaRepository;
import com.tagnote.infrastructure.persistence.catalog.TrackJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CatalogSubjectTagsReadServiceTest {
    private TrackJpaRepository tracks;
    private AlbumJpaRepository albums;
    private TrackDetailReadService trackDetails;
    private AlbumDetailReadService albumDetails;
    private TagPreviewService previews;
    private CatalogSubjectTagsReadService service;

    @BeforeEach
    void 준비() {
        tracks = mock(TrackJpaRepository.class);
        albums = mock(AlbumJpaRepository.class);
        trackDetails = mock(TrackDetailReadService.class);
        albumDetails = mock(AlbumDetailReadService.class);
        previews = mock(TagPreviewService.class);
        service = new CatalogSubjectTagsReadService(tracks, albums, trackDetails, albumDetails, previews);
    }

    @Test
    void 트랙의_저장된_확정_태그가_있으면_미리보기를_읽지_않는다() {
        TrackEntity track = mock(TrackEntity.class);
        when(track.getTrackId()).thenReturn(10L);
        when(tracks.findBySpotifyId("track-1")).thenReturn(Optional.of(track));
        when(trackDetails.findResolvedTags(10L)).thenReturn(Optional.of(
                List.of(new SystemTagDetail(11L, "Rock", 0.9))));

        SubjectTags result = service.getTrackTags("track-1");

        assertThat(result.tagDisplayStatus()).isEqualTo(TagDisplayStatus.CONFIRMED);
        assertThat(result.systemTags()).extracting(SystemTagDetail::name).containsExactly("Rock");
        assertThat(result.previewTags()).isEmpty();
        verify(previews, never()).fromPersisted(10L, 5L);
    }

    @Test
    void 트랙에_확정_태그가_없으면_저장된_트랙과_앨범_미리보기를_읽는다() {
        AlbumEntity album = mock(AlbumEntity.class);
        when(album.getAlbumId()).thenReturn(5L);
        TrackEntity track = mock(TrackEntity.class);
        when(track.getTrackId()).thenReturn(10L);
        when(track.getAlbum()).thenReturn(album);
        when(tracks.findBySpotifyId("track-1")).thenReturn(Optional.of(track));
        when(trackDetails.findResolvedTags(10L)).thenReturn(Optional.empty());
        when(previews.fromPersisted(10L, 5L)).thenReturn(List.of(
                new PreviewTagDetail("Dream Pop", ExternalTagSource.LASTFM)));

        SubjectTags result = service.getTrackTags("track-1");

        assertThat(result.tagDisplayStatus()).isEqualTo(TagDisplayStatus.PREVIEW);
        assertThat(result.previewTags()).extracting(PreviewTagDetail::name).containsExactly("Dream Pop");
        verify(previews).fromPersisted(10L, 5L);
    }

    @Test
    void 숨김_결과만_있는_앨범은_미리보기를_노출하지_않는다() {
        AlbumEntity album = mock(AlbumEntity.class);
        when(album.getAlbumId()).thenReturn(5L);
        when(albums.findBySpotifyId("album-1")).thenReturn(Optional.of(album));
        when(albumDetails.findResolvedTags(5L)).thenReturn(Optional.of(List.of()));

        SubjectTags result = service.getAlbumTags("album-1");

        assertThat(result.tagDisplayStatus()).isEqualTo(TagDisplayStatus.EMPTY);
        assertThat(result.previewTags()).isEmpty();
        verify(previews, never()).fromPersistedAlbum(5L);
    }

    @Test
    void 앨범에_확정_태그가_없으면_저장된_앨범_미리보기를_읽는다() {
        AlbumEntity album = mock(AlbumEntity.class);
        when(album.getAlbumId()).thenReturn(5L);
        when(albums.findBySpotifyId("album-1")).thenReturn(Optional.of(album));
        when(albumDetails.findResolvedTags(5L)).thenReturn(Optional.empty());
        when(previews.fromPersistedAlbum(5L)).thenReturn(List.of(
                new PreviewTagDetail("Ambient", ExternalTagSource.LASTFM)));

        SubjectTags result = service.getAlbumTags("album-1");

        assertThat(result.tagDisplayStatus()).isEqualTo(TagDisplayStatus.PREVIEW);
        assertThat(result.previewTags()).extracting(PreviewTagDetail::name).containsExactly("Ambient");
        verify(previews).fromPersistedAlbum(5L);
    }

    @Test
    void import_전_대상이_없으면_기존_Catalog_오류를_반환한다() {
        when(tracks.findBySpotifyId("track-1")).thenReturn(Optional.empty());
        when(albums.findBySpotifyId("album-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getTrackTags("track-1"))
                .isInstanceOfSatisfying(CustomException.class,
                        error -> assertThat(error.getExceptionCode()).isEqualTo(ExceptionCode.TRACK_NOT_FOUND));
        assertThatThrownBy(() -> service.getAlbumTags("album-1"))
                .isInstanceOfSatisfying(CustomException.class,
                        error -> assertThat(error.getExceptionCode()).isEqualTo(ExceptionCode.ALBUM_NOT_FOUND));
    }
}
