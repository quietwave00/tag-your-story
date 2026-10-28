package com.tagnote.api.domain.catalog;

import com.tagnote.api.support.WebMvcMethodSecurityTestConfig;
import com.tagnote.application.catalog.search.CatalogSearchService;
import com.tagnote.application.catalog.search.model.CatalogSearchItem;
import com.tagnote.application.catalog.search.model.CatalogSearchResult;
import com.tagnote.application.catalog.search.model.CatalogSearchScope;
import com.tagnote.application.catalog.search.model.CatalogSearchSubjectType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CatalogSearchController.class)
@Import(WebMvcMethodSecurityTestConfig.class)
class CatalogSearchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CatalogSearchService catalogSearchService;

    @Test
    void 타입을_생략한_카탈로그_검색은_기존_통합_검색_응답을_유지한다() throws Exception {
        when(catalogSearchService.search("Queen", 0, CatalogSearchScope.ALL)).thenReturn(CatalogSearchResult.of(List.of(
                CatalogSearchItem.of(
                        CatalogSearchSubjectType.TRACK,
                        "track-1",
                        "Bohemian Rhapsody",
                        "Queen",
                        "A Night At The Opera",
                        "image",
                        0
                ),
                CatalogSearchItem.of(
                        CatalogSearchSubjectType.ARTIST,
                        "artist-1",
                        "Queen",
                        null,
                        null,
                        "artist-image",
                        0
                )
        ), 42L));

        mockMvc.perform(get("/api/catalog/search")
                        .param("keyword", "Queen")
                        .param("page", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.response.items[0].subjectType").value("TRACK"))
                .andExpect(jsonPath("$.response.items[0].spotifyId").value("track-1"))
                .andExpect(jsonPath("$.response.items[0].title").value("Bohemian Rhapsody"))
                .andExpect(jsonPath("$.response.items[0].artistName").value("Queen"))
                .andExpect(jsonPath("$.response.items[0].albumName").value("A Night At The Opera"))
                .andExpect(jsonPath("$.response.items[0].imageUrl").value("image"))
                .andExpect(jsonPath("$.response.items[1].subjectType").value("ARTIST"))
                .andExpect(jsonPath("$.response.items[1].spotifyId").value("artist-1"))
                .andExpect(jsonPath("$.response.totalCount").value(42));
        verify(catalogSearchService).search("Queen", 0, CatalogSearchScope.ALL);
    }

    @Test
    void 카탈로그_검색은_필수_요청값이_없으면_400을_반환한다() throws Exception {
        mockMvc.perform(get("/api/catalog/search").param("keyword", "Queen"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/catalog/search").param("page", "0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void 타입별_카탈로그_검색은_선택한_타입을_전달하고_해당_결과를_반환한다() throws Exception {
        for (CatalogSearchScope scope : List.of(CatalogSearchScope.TRACK,
                CatalogSearchScope.ALBUM, CatalogSearchScope.ARTIST)) {
            when(catalogSearchService.search("Queen", 1, scope)).thenReturn(CatalogSearchResult.of(List.of(
                    CatalogSearchItem.of(CatalogSearchSubjectType.valueOf(scope.name()),
                            scope.name().toLowerCase() + "-1", "Queen", null, null, "image", 0)), 17L));

            mockMvc.perform(get("/api/catalog/search")
                            .param("keyword", "Queen").param("page", "1").param("type", scope.name()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.response.items[0].subjectType").value(scope.name()))
                    .andExpect(jsonPath("$.response.totalCount").value(17));
            verify(catalogSearchService).search("Queen", 1, scope);
        }
    }

    @Test
    void 허용되지_않은_검색_타입은_400을_반환한다() throws Exception {
        mockMvc.perform(get("/api/catalog/search")
                        .param("keyword", "Queen").param("page", "0").param("type", "PODCAST"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/catalog/search")
                        .param("keyword", "Queen").param("page", "0").param("type", ""))
                .andExpect(status().isBadRequest());
    }

    @Test
    void 타입별_검색은_Spotify_offset_범위를_벗어난_페이지를_차단한다() throws Exception {
        mockMvc.perform(get("/api/catalog/search")
                        .param("keyword", "in love").param("page", "-1").param("type", "TRACK"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/catalog/search")
                        .param("keyword", "in love").param("page", "101").param("type", "TRACK"))
                .andExpect(status().isBadRequest());
    }
}
