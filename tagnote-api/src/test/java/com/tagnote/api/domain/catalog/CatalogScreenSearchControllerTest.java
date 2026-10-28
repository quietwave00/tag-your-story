package com.tagnote.api.domain.catalog;

import com.tagnote.api.support.WebMvcMethodSecurityTestConfig;
import com.tagnote.application.catalog.search.CatalogScreenSearchService;
import com.tagnote.application.catalog.search.model.CatalogScreenSearchResult;
import com.tagnote.application.catalog.search.model.CatalogSearchItem;
import com.tagnote.application.catalog.search.model.CatalogSearchSubjectType;
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
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CatalogScreenSearchController.class)
@Import(WebMvcMethodSecurityTestConfig.class)
class CatalogScreenSearchControllerTest {
    @Autowired private MockMvc mockMvc;
    @MockBean private CatalogScreenSearchService service;

    @Test
    void 화면_검색은_세_타입의_독립_페이지와_다음_페이지_여부를_반환한다() throws Exception {
        CatalogSearchItem track = CatalogSearchItem.of(CatalogSearchSubjectType.TRACK,
                "track-1", "Queen", "Band", "Album", "image", 2);
        when(service.search("Queen", 1, 2, 0)).thenReturn(new CatalogScreenSearchResult(
                new CatalogScreenSearchResult.Section(List.of(track), 71, true),
                new CatalogScreenSearchResult.Section(List.of(), 30, false),
                new CatalogScreenSearchResult.Section(List.of(), 4, false)));

        mockMvc.perform(get("/api/catalog/search/screen").param("keyword", "Queen")
                        .param("trackPage", "1").param("albumPage", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.response.tracks.items[0].spotifyId").value("track-1"))
                .andExpect(jsonPath("$.response.tracks.items[0].subjectType").value("TRACK"))
                .andExpect(jsonPath("$.response.tracks.totalCount").value(71))
                .andExpect(jsonPath("$.response.tracks.hasNext").value(true))
                .andExpect(jsonPath("$.response.albums.items").isEmpty())
                .andExpect(jsonPath("$.response.artists.totalCount").value(4));
        verify(service).search("Queen", 1, 2, 0);
    }

    @Test
    void 범위를_벗어난_화면_페이지는_400으로_차단한다() throws Exception {
        mockMvc.perform(get("/api/catalog/search/screen").param("keyword", "Queen")
                        .param("trackPage", "-1"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/catalog/search/screen").param("keyword", "Queen")
                        .param("albumPage", "101"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/catalog/search/screen").param("keyword", "Queen")
                        .param("artistPage", "abc"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void 화면_검색의_Spotify_장애는_기존_오류_계약을_유지한다() throws Exception {
        when(service.search("unavailable", 0, 0, 0))
                .thenThrow(new CustomException(ExceptionCode.SPOTIFY_EXCEPTION));

        mockMvc.perform(get("/api/catalog/search/screen").param("keyword", "unavailable"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.response.exceptionCode").value("SPOTIFY_EXCEPTION"));
    }
}
