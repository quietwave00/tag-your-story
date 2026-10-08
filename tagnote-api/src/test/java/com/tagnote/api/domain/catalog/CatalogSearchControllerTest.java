package com.tagnote.api.domain.catalog;

import com.tagnote.api.support.WebMvcMethodSecurityTestConfig;
import com.tagnote.application.catalog.search.CatalogSearchService;
import com.tagnote.application.catalog.search.model.CatalogSearchItem;
import com.tagnote.application.catalog.search.model.CatalogSearchResult;
import com.tagnote.application.catalog.search.model.CatalogSearchSubjectType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
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
    void GET_api_catalog_search는_통합검색_contract를_반환한다() throws Exception {
        when(catalogSearchService.search("Queen", 0)).thenReturn(CatalogSearchResult.of(List.of(
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
    }

    @Test
    void GET_api_catalog_search는_필수_query가_없으면_400을_반환한다() throws Exception {
        mockMvc.perform(get("/api/catalog/search").param("keyword", "Queen"))
                .andExpect(status().isBadRequest());
    }
}
