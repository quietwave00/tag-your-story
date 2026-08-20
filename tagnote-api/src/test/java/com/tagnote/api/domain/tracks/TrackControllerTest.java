package com.tagnote.api.domain.tracks;

import com.tagnote.api.support.WebMvcMethodSecurityTestConfig;
import com.tagnote.application.catalog.importer.TrackImportService;
import com.tagnote.application.catalog.importer.model.ImportedAlbum;
import com.tagnote.application.catalog.importer.model.ImportedArtist;
import com.tagnote.application.catalog.importer.model.ImportedTrack;
import com.tagnote.application.catalog.search.TrackSearchService;
import com.tagnote.application.catalog.search.model.TrackSearchItem;
import com.tagnote.application.catalog.search.model.TrackSearchResult;
import com.tagnote.core.domain.tracks.service.TrackService;
import com.tagnote.core.domain.tracks.service.dto.TrackData;
import com.tagnote.core.domain.tracks.service.dto.response.RankingList;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TrackController.class)
@Import(WebMvcMethodSecurityTestConfig.class)
class TrackControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TrackSearchService trackSearchService;

    @MockBean
    private TrackService trackService;

    @MockBean
    private TrackImportService trackImportService;

    @Test
    void POST_api_tracks_import는_인증없이_전체_Artist_credit을_반환한다() throws Exception {
        ImportedArtist trackArtist = ImportedArtist.of(3L, "track-artist", "Track Artist", 0);
        ImportedArtist albumArtist = ImportedArtist.of(7L, "album-artist", "Album Artist", 0);
        ImportedAlbum album = ImportedAlbum.of(
                5L,
                "album-1",
                "album",
                2024,
                List.of(albumArtist)
        );
        when(trackImportService.importTrack("track-1")).thenReturn(
                ImportedTrack.of(
                        10L,
                        "track-1",
                        "title",
                        "ISRC-1",
                        240_000,
                        List.of(trackArtist),
                        album
                )
        );

        mockMvc.perform(post("/api/tracks/import")
                        .contentType(APPLICATION_JSON)
                        .content("{\"spotifyTrackId\":\"track-1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.response.catalogTrackId").value(10))
                .andExpect(jsonPath("$.response.spotifyTrackId").value("track-1"))
                .andExpect(jsonPath("$.response.artists[0].spotifyArtistId").value("track-artist"))
                .andExpect(jsonPath("$.response.artists[0].position").value(0))
                .andExpect(jsonPath("$.response.album.albumId").value(5))
                .andExpect(jsonPath("$.response.album.artists[0].spotifyArtistId").value("album-artist"));
    }

    @Test
    void POST_api_tracks_import는_blank_spotifyTrackId를_거부한다() throws Exception {
        mockMvc.perform(post("/api/tracks/import")
                        .contentType(APPLICATION_JSON)
                        .content("{\"spotifyTrackId\":\" \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.response.message").value("spotifyTrackId는 비어 있을 수 없습니다."))
                .andExpect(jsonPath("$.response.status").value(400));
    }

    @Test
    void POST_api_tracks_import는_요청_body_누락을_400으로_반환한다() throws Exception {
        mockMvc.perform(post("/api/tracks/import")
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.response.message").value("요청 본문을 읽을 수 없습니다."))
                .andExpect(jsonPath("$.response.status").value(400));
    }

    @Test
    void GET_api_tracks는_keyword와_page_query를_유지한다() throws Exception {
        when(trackSearchService.search("rock", 0)).thenReturn(
                TrackSearchResult.of(
                        List.of(TrackSearchItem.of("track-1", "artist", "title", "album", "image")),
                        42
                )
        );

        mockMvc.perform(get("/api/tracks").param("keyword", "rock").param("page", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.response.trackDataList[0].trackId").value("track-1"))
                .andExpect(jsonPath("$.response.trackDataList[0].artistName").value("artist"))
                .andExpect(jsonPath("$.response.trackDataList[0].title").value("title"))
                .andExpect(jsonPath("$.response.trackDataList[0].albumName").value("album"))
                .andExpect(jsonPath("$.response.trackDataList[0].imageUrl").value("image"))
                .andExpect(jsonPath("$.response.totalCount").value(42));
    }

    @Test
    void GET_api_tracks_trackId는_TrackData_shape를_그대로_반환한다() throws Exception {
        when(trackService.getDetail("track-1")).thenReturn(TrackData.of("track-1", "artist", "title", "album", "image"));

        mockMvc.perform(get("/api/tracks/{trackId}", "track-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.response.trackId").value("track-1"))
                .andExpect(jsonPath("$.response.artistName").value("artist"))
                .andExpect(jsonPath("$.response.title").value("title"))
                .andExpect(jsonPath("$.response.albumName").value("album"))
                .andExpect(jsonPath("$.response.imageUrl").value("image"));
    }

    @Test
    void GET_api_tracks_ranking_route를_유지한다() throws Exception {
        when(trackService.getKeywordRanking()).thenReturn(RankingList.onComplete(List.of("rock", "pop")));

        mockMvc.perform(get("/api/tracks/ranking"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.response.keywordList[0]").value("rock"))
                .andExpect(jsonPath("$.response.keywordList[1]").value("pop"));
    }
}
