package com.tagnote.api.domain.catalog;

import com.tagnote.api.support.WebMvcMethodSecurityTestConfig;
import com.tagnote.application.catalog.detail.CatalogDetailService;
import com.tagnote.application.catalog.detail.model.CatalogReleaseType;
import com.tagnote.application.catalog.detail.model.SpotifyAlbumDetail;
import com.tagnote.application.catalog.detail.model.SpotifyAlbumSummary;
import com.tagnote.application.catalog.detail.model.SpotifyAlbumTrack;
import com.tagnote.application.catalog.detail.model.SpotifyArtistDetail;
import com.tagnote.application.catalog.detail.model.SpotifyCatalogPage;
import com.tagnote.core.exception.CustomException;
import com.tagnote.core.exception.ExceptionCode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CatalogDetailController.class)
@Import(WebMvcMethodSecurityTestConfig.class)
class CatalogDetailControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CatalogDetailService catalogDetailService;

    @Test
    void 앨범_상세는_Spotify_ID로_조회하고_참여_아티스트를_응답한다() throws Exception {
        when(catalogDetailService.getAlbum("album-1")).thenReturn(new SpotifyAlbumDetail(
                "album-1", "Album", "cover", "2024-03", 12,
                List.of(new SpotifyAlbumDetail.ArtistCredit("artist-1", "Queen")), "album", 1_800_000L));

        mockMvc.perform(get("/api/catalog/albums/album-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.response.spotifyId").value("album-1"))
                .andExpect(jsonPath("$.response.title").value("Album"))
                .andExpect(jsonPath("$.response.imageUrl").value("cover"))
                .andExpect(jsonPath("$.response.releaseDate").value("2024-03"))
                .andExpect(jsonPath("$.response.totalTracks").value(12))
                .andExpect(jsonPath("$.response.releaseType").value("ALBUM"))
                .andExpect(jsonPath("$.response.artists[0].spotifyId").value("artist-1"))
                .andExpect(jsonPath("$.response.artists[0].name").value("Queen"));
        verify(catalogDetailService).getAlbum("album-1");
    }

    @Test
    void 아티스트_상세는_팔로워_수_없이_장르와_이미지를_응답한다() throws Exception {
        when(catalogDetailService.getArtist("artist-1")).thenReturn(new SpotifyArtistDetail(
                "artist-1", "Queen", "image", List.of("classic rock")));

        mockMvc.perform(get("/api/catalog/artists/artist-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.response.spotifyId").value("artist-1"))
                .andExpect(jsonPath("$.response.name").value("Queen"))
                .andExpect(jsonPath("$.response.imageUrl").value("image"))
                .andExpect(jsonPath("$.response.genres[0]").value("classic rock"))
                .andExpect(jsonPath("$.response.followers").doesNotExist());
        verify(catalogDetailService).getArtist("artist-1");
    }

    @Test
    void Spotify_조회_실패는_기존_오류_계약을_유지한다() throws Exception {
        when(catalogDetailService.getArtist("unavailable"))
                .thenThrow(new CustomException(ExceptionCode.SPOTIFY_EXCEPTION));

        mockMvc.perform(get("/api/catalog/artists/unavailable"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.response.exceptionCode").value("SPOTIFY_EXCEPTION"))
                .andExpect(jsonPath("$.response.status").value(503));
    }

    @Test
    void 아티스트_앨범_목록은_선택한_페이지와_Spotify_전체_건수를_반환한다() throws Exception {
        when(catalogDetailService.getArtistAlbums("artist-1", 2)).thenReturn(new SpotifyCatalogPage<>(
                List.of(new SpotifyAlbumSummary("album-1", "Album", "cover", "2024-03", CatalogReleaseType.SINGLE_EP)), 31));

        mockMvc.perform(get("/api/catalog/artists/artist-1/albums").param("page", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.response.items[0].spotifyId").value("album-1"))
                .andExpect(jsonPath("$.response.items[0].title").value("Album"))
                .andExpect(jsonPath("$.response.items[0].imageUrl").value("cover"))
                .andExpect(jsonPath("$.response.items[0].releaseDate").value("2024-03"))
                .andExpect(jsonPath("$.response.items[0].releaseType").value("SINGLE_EP"))
                .andExpect(jsonPath("$.response.totalCount").value(31));
        verify(catalogDetailService).getArtistAlbums("artist-1", 2);
    }

    @Test
    void 앨범_트랙_목록은_기본_첫_페이지와_빈_결과를_반환한다() throws Exception {
        when(catalogDetailService.getAlbumTracks("album-1", 0))
                .thenReturn(new SpotifyCatalogPage<>(List.of(), 0));

        mockMvc.perform(get("/api/catalog/albums/album-1/tracks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.response.items").isEmpty())
                .andExpect(jsonPath("$.response.totalCount").value(0));
        verify(catalogDetailService).getAlbumTracks("album-1", 0);
    }

    @Test
    void 앨범_트랙_목록은_트랙_순서와_번호를_반환한다() throws Exception {
        when(catalogDetailService.getAlbumTracks("album-1", 1)).thenReturn(new SpotifyCatalogPage<>(
                List.of(new SpotifyAlbumTrack("track-1", "Song", "Queen", 2, 3, 180000)), 17));

        mockMvc.perform(get("/api/catalog/albums/album-1/tracks").param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.response.items[0].spotifyId").value("track-1"))
                .andExpect(jsonPath("$.response.items[0].artistName").value("Queen"))
                .andExpect(jsonPath("$.response.items[0].discNumber").value(2))
                .andExpect(jsonPath("$.response.items[0].trackNumber").value(3))
                .andExpect(jsonPath("$.response.items[0].durationMs").value(180000))
                .andExpect(jsonPath("$.response.totalCount").value(17));
    }

    @Test
    void 목록_페이지가_유효하지_않으면_400을_반환한다() throws Exception {
        mockMvc.perform(get("/api/catalog/artists/artist-1/albums").param("page", "-1"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/catalog/albums/album-1/tracks").param("page", "214748365"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/catalog/albums/album-1/tracks").param("page", "abc"))
                .andExpect(status().isBadRequest());
    }
}
