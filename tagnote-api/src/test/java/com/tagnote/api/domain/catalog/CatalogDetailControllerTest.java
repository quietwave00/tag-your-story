package com.tagnote.api.domain.catalog;

import com.tagnote.api.support.WebMvcMethodSecurityTestConfig;
import com.tagnote.application.catalog.detail.CatalogDetailService;
import com.tagnote.application.catalog.detail.model.SpotifyAlbumDetail;
import com.tagnote.application.catalog.detail.model.SpotifyArtistDetail;
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
                List.of(new SpotifyAlbumDetail.ArtistCredit("artist-1", "Queen"))));

        mockMvc.perform(get("/api/catalog/albums/album-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.response.spotifyId").value("album-1"))
                .andExpect(jsonPath("$.response.title").value("Album"))
                .andExpect(jsonPath("$.response.imageUrl").value("cover"))
                .andExpect(jsonPath("$.response.releaseDate").value("2024-03"))
                .andExpect(jsonPath("$.response.totalTracks").value(12))
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
}
