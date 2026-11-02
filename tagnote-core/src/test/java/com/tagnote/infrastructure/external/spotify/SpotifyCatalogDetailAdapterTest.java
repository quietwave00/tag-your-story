package com.tagnote.infrastructure.external.spotify;

import com.tagnote.application.catalog.detail.model.CatalogReleaseType;
import com.tagnote.application.catalog.detail.model.SpotifyAlbumSummary;
import com.tagnote.application.catalog.detail.model.SpotifyAlbumTrack;
import com.tagnote.core.exception.CustomException;
import com.tagnote.core.exception.ExceptionCode;
import com.tagnote.infrastructure.external.spotify.dto.SpotifyJson;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class SpotifyCatalogDetailAdapterTest {
    private final SpotifyHttpClient client = mock(SpotifyHttpClient.class);
    private final SpotifyCatalogDetailAdapter adapter = new SpotifyCatalogDetailAdapter(client);

    @Test
    void album_detail_preserves_credits_and_embedded_duration_classification() {
        com.tagnote.infrastructure.external.spotify.dto.SpotifyJson.Album album = SpotifyFixtures.read("""
                {"id":"al","name":"Album","album_type":"album","release_date":"2024-03-15",
                 "artists":[{"id":"a","name":"A"},{"id":"b","name":"B"}],"images":[{"url":"cover"}],
                 "tracks":{"total":7,"items":[{"duration_ms":300000},{"duration_ms":300000},{"duration_ms":300000},
                 {"duration_ms":300000},{"duration_ms":300000},{"duration_ms":300000},{"duration_ms":300000}]}}
                """, SpotifyJson.Album.class);
        when(client.album("al")).thenReturn(album);
        com.tagnote.application.catalog.detail.model.SpotifyAlbumDetail detail = adapter.getAlbum("al");
        assertThat(detail.artists()).extracting(a -> a.spotifyId()).containsExactly("a", "b");
        assertThat(detail.releaseType()).isEqualTo(CatalogReleaseType.ALBUM);
        assertThat(detail.imageUrl()).isEqualTo("cover");
        verify(client, never()).albumTracks(anyString(), anyInt());
    }

    @Test
    void artist_album_list_classifies_six_tracks_without_extra_call_and_preserves_partial_success() {
        com.tagnote.infrastructure.external.spotify.dto.SpotifyJson.Page<com.tagnote.infrastructure.external.spotify.dto.SpotifyJson.Album> page = SpotifyFixtures.read("""
                {"total":3,"items":[
                  {"id":"short","name":"Short","album_type":"album","total_tracks":6,"images":[]},
                  {"id":"failed","name":"Failed","album_type":"album","total_tracks":7,"images":[]},
                  {"id":"single","name":"Single","album_type":"single","total_tracks":1,"images":[]}]}
                """, new com.fasterxml.jackson.core.type.TypeReference<SpotifyJson.Page<SpotifyJson.Album>>() {});
        when(client.artistAlbums("ar", 0)).thenReturn(page);
        when(client.albumTracks("failed", 0)).thenThrow(new CustomException(ExceptionCode.SPOTIFY_EXCEPTION));
        com.tagnote.application.catalog.detail.model.SpotifyCatalogPage<com.tagnote.application.catalog.detail.model.SpotifyAlbumSummary> result = adapter.getArtistAlbums("ar", 0);
        assertThat(result.items()).extracting(SpotifyAlbumSummary::releaseType)
                .containsExactly(CatalogReleaseType.SINGLE_EP, null, CatalogReleaseType.SINGLE_EP);
        assertThat(result.totalCount()).isEqualTo(3);
        verify(client, never()).albumTracks("short", 0);
        verify(client, never()).albumTracks("single", 0);
    }

    @Test
    void artist_album_list_uses_thirty_minute_boundary_for_seven_tracks() {
        com.tagnote.infrastructure.external.spotify.dto.SpotifyJson.Page<com.tagnote.infrastructure.external.spotify.dto.SpotifyJson.Album> albums = SpotifyFixtures.read("""
                {"total":2,"items":[
                  {"id":"short","name":"Short","album_type":"album","total_tracks":7,"images":[]},
                  {"id":"long","name":"Long","album_type":"album","total_tracks":7,"images":[]}]}
                """, new com.fasterxml.jackson.core.type.TypeReference<SpotifyJson.Page<SpotifyJson.Album>>() {});
        com.tagnote.infrastructure.external.spotify.dto.SpotifyJson.Page<com.tagnote.infrastructure.external.spotify.dto.SpotifyJson.Track> shortTracks = SpotifyFixtures.read("""
                {"total":7,"items":[{"duration_ms":240000},{"duration_ms":240000},{"duration_ms":240000},
                {"duration_ms":240000},{"duration_ms":240000},{"duration_ms":240000},{"duration_ms":240000}]}
                """, new com.fasterxml.jackson.core.type.TypeReference<SpotifyJson.Page<SpotifyJson.Track>>() {});
        com.tagnote.infrastructure.external.spotify.dto.SpotifyJson.Page<com.tagnote.infrastructure.external.spotify.dto.SpotifyJson.Track> longTracks = SpotifyFixtures.read("""
                {"total":7,"items":[{"duration_ms":300000},{"duration_ms":300000},{"duration_ms":300000},
                {"duration_ms":300000},{"duration_ms":300000},{"duration_ms":300000},{"duration_ms":300000}]}
                """, new com.fasterxml.jackson.core.type.TypeReference<SpotifyJson.Page<SpotifyJson.Track>>() {});
        when(client.artistAlbums("ar", 0)).thenReturn(albums);
        when(client.albumTracks("short", 0)).thenReturn(shortTracks);
        when(client.albumTracks("long", 0)).thenReturn(longTracks);
        com.tagnote.application.catalog.detail.model.SpotifyCatalogPage<com.tagnote.application.catalog.detail.model.SpotifyAlbumSummary> result = adapter.getArtistAlbums("ar", 0);
        assertThat(result.items()).extracting(SpotifyAlbumSummary::releaseType)
                .containsExactly(CatalogReleaseType.SINGLE_EP, CatalogReleaseType.ALBUM);
        verify(client, never()).albumTracks("short", 1);
        verify(client, never()).albumTracks("long", 1);
    }

    @Test
    void 싱글과_컴필레이션은_트랙을_조회하지_않고_원본_순서와_건수를_유지한다() {
        com.tagnote.infrastructure.external.spotify.dto.SpotifyJson.Page<com.tagnote.infrastructure.external.spotify.dto.SpotifyJson.Album> albums = SpotifyFixtures.read("""
                {"total":12,"items":[
                  {"id":"comp","name":"Compilation","album_type":"compilation","images":[]},
                  {"id":"single","name":"Single","album_type":"single","images":[]},
                  {"id":"unknown","name":"Unknown","album_type":"other","images":[]}]}
                """, new com.fasterxml.jackson.core.type.TypeReference<SpotifyJson.Page<SpotifyJson.Album>>() {});
        when(client.artistAlbums("ar", 1)).thenReturn(albums);

        com.tagnote.application.catalog.detail.model.SpotifyCatalogPage<com.tagnote.application.catalog.detail.model.SpotifyAlbumSummary> result = adapter.getArtistAlbums("ar", 1);

        assertThat(result.items()).extracting(SpotifyAlbumSummary::spotifyId)
                .containsExactly("comp", "single", "unknown");
        assertThat(result.items()).extracting(SpotifyAlbumSummary::releaseType)
                .containsExactly(CatalogReleaseType.COMPILATION, CatalogReleaseType.SINGLE_EP, null);
        assertThat(result.totalCount()).isEqualTo(12);
        verify(client, never()).albumTracks(anyString(), anyInt());
    }

    @Test
    void 열한_곡의_후속_페이지까지_읽고_정확히_삼십_분이면_앨범으로_분류한다() {
        com.tagnote.infrastructure.external.spotify.dto.SpotifyJson.Page<com.tagnote.infrastructure.external.spotify.dto.SpotifyJson.Album> albums = SpotifyFixtures.read("""
                {"total":1,"items":[{"id":"al","name":"Album","album_type":"album","total_tracks":11,"images":[]}]}
                """, new com.fasterxml.jackson.core.type.TypeReference<SpotifyJson.Page<SpotifyJson.Album>>() {});
        com.tagnote.infrastructure.external.spotify.dto.SpotifyJson.Page<com.tagnote.infrastructure.external.spotify.dto.SpotifyJson.Track> first = SpotifyFixtures.read("""
                {"total":11,"items":[{"duration_ms":150000},{"duration_ms":150000},
                {"duration_ms":150000},{"duration_ms":150000},{"duration_ms":150000},
                {"duration_ms":150000},{"duration_ms":150000},{"duration_ms":150000},
                {"duration_ms":150000},{"duration_ms":150000}]}
                """, new com.fasterxml.jackson.core.type.TypeReference<SpotifyJson.Page<SpotifyJson.Track>>() {});
        com.tagnote.infrastructure.external.spotify.dto.SpotifyJson.Page<com.tagnote.infrastructure.external.spotify.dto.SpotifyJson.Track> second = SpotifyFixtures.read("""
                {"total":11,"items":[{"duration_ms":300000}]}
                """, new com.fasterxml.jackson.core.type.TypeReference<SpotifyJson.Page<SpotifyJson.Track>>() {});
        when(client.artistAlbums("ar", 0)).thenReturn(albums);
        when(client.albumTracks("al", 0)).thenReturn(first);
        when(client.albumTracks("al", 1)).thenReturn(second);

        com.tagnote.application.catalog.detail.model.SpotifyCatalogPage<com.tagnote.application.catalog.detail.model.SpotifyAlbumSummary> result = adapter.getArtistAlbums("ar", 0);

        assertThat(result.items().get(0).releaseType()).isEqualTo(CatalogReleaseType.ALBUM);
        verify(client).albumTracks("al", 1);
        verify(client, never()).albumTracks("al", 2);
    }

    @Test
    void 트랙_총수_불일치와_불완전한_첫_페이지는_분류하지_않는다() {
        com.tagnote.infrastructure.external.spotify.dto.SpotifyJson.Page<com.tagnote.infrastructure.external.spotify.dto.SpotifyJson.Album> albums = SpotifyFixtures.read("""
                {"total":2,"items":[
                  {"id":"mismatch","name":"Mismatch","album_type":"album","total_tracks":7,"images":[]},
                  {"id":"partial","name":"Partial","album_type":"album","total_tracks":11,"images":[]}]}
                """, new com.fasterxml.jackson.core.type.TypeReference<SpotifyJson.Page<SpotifyJson.Album>>() {});
        com.tagnote.infrastructure.external.spotify.dto.SpotifyJson.Page<com.tagnote.infrastructure.external.spotify.dto.SpotifyJson.Track> mismatch = SpotifyFixtures.read("""
                {"total":8,"items":[{"duration_ms":300000}]}
                """, new com.fasterxml.jackson.core.type.TypeReference<SpotifyJson.Page<SpotifyJson.Track>>() {});
        com.tagnote.infrastructure.external.spotify.dto.SpotifyJson.Page<com.tagnote.infrastructure.external.spotify.dto.SpotifyJson.Track> partial = SpotifyFixtures.read("""
                {"total":11,"items":[{"duration_ms":1800000}]}
                """, new com.fasterxml.jackson.core.type.TypeReference<SpotifyJson.Page<SpotifyJson.Track>>() {});
        when(client.artistAlbums("ar", 0)).thenReturn(albums);
        when(client.albumTracks("mismatch", 0)).thenReturn(mismatch);
        when(client.albumTracks("partial", 0)).thenReturn(partial);

        com.tagnote.application.catalog.detail.model.SpotifyCatalogPage<com.tagnote.application.catalog.detail.model.SpotifyAlbumSummary> result = adapter.getArtistAlbums("ar", 0);

        assertThat(result.items()).extracting(SpotifyAlbumSummary::releaseType)
                .containsExactly(null, null);
        verify(client, never()).albumTracks("partial", 1);
    }

    @Test
    void 원본_아티스트_앨범_목록_오류는_그대로_전파한다() {
        when(client.artistAlbums("ar", 0)).thenThrow(new CustomException(ExceptionCode.SPOTIFY_EXCEPTION));

        assertThatThrownBy(() -> adapter.getArtistAlbums("ar", 0))
                .isInstanceOf(CustomException.class);
    }

    @Test
    void album_track_page_preserves_order_disc_and_duration() {
        com.tagnote.infrastructure.external.spotify.dto.SpotifyJson.Page<com.tagnote.infrastructure.external.spotify.dto.SpotifyJson.Track> page = SpotifyFixtures.read("""
                {"total":12,"items":[
                 {"id":"t1","name":"One","artists":[{"name":"A"}],"disc_number":2,"track_number":3,"duration_ms":180000},
                 {"id":"t2","name":"Two","artists":[],"disc_number":2,"track_number":4,"duration_ms":100000}]}
                """, new com.fasterxml.jackson.core.type.TypeReference<SpotifyJson.Page<SpotifyJson.Track>>() {});
        when(client.albumTracks("al", 1)).thenReturn(page);
        com.tagnote.application.catalog.detail.model.SpotifyCatalogPage<com.tagnote.application.catalog.detail.model.SpotifyAlbumTrack> result = adapter.getAlbumTracks("al", 1);
        assertThat(result.items()).extracting(SpotifyAlbumTrack::spotifyId).containsExactly("t1", "t2");
        assertThat(result.items().get(0).discNumber()).isEqualTo(2);
        assertThat(result.items().get(0).durationMs()).isEqualTo(180000);
        assertThat(result.items().get(1).artistName()).isNull();
        assertThat(result.totalCount()).isEqualTo(12);
    }
}
