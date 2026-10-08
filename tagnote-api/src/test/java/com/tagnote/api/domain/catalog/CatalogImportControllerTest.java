package com.tagnote.api.domain.catalog;

import com.tagnote.api.support.WebMvcMethodSecurityTestConfig;
import com.tagnote.application.catalog.detail.model.AlbumDetail;
import com.tagnote.application.catalog.detail.model.SystemTagDetail;
import com.tagnote.application.catalog.detail.model.TrackDetail;
import com.tagnote.application.catalog.importer.model.ImportedAlbum;
import com.tagnote.application.catalog.importer.model.ImportedTrack;
import com.tagnote.application.catalog.selection.AlbumSelectionService;
import com.tagnote.application.catalog.selection.TrackSelectionService;
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
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CatalogImportController.class)
@Import(WebMvcMethodSecurityTestConfig.class)
class CatalogImportControllerTest {
    @Autowired MockMvc mvc;
    @MockBean TrackSelectionService tracks;
    @MockBean AlbumSelectionService albums;

    @Test
    void album_id는_album_경로로_전달한다() throws Exception {
        ImportedAlbum album = ImportedAlbum.of(5L, "album-1", "Album", 2024, List.of());
        when(albums.select("album-1")).thenReturn(new AlbumDetail(album,
                List.of(new SystemTagDetail(11L, "Rock", 0.9))));

        mvc.perform(post("/api/catalog/import").contentType(APPLICATION_JSON)
                        .content("{\"subjectType\":\"ALBUM\",\"spotifyId\":\"album-1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.response.subjectType").value("ALBUM"))
                .andExpect(jsonPath("$.response.album.album.spotifyAlbumId").value("album-1"))
                .andExpect(jsonPath("$.response.album.systemTags[0].name").value("Rock"))
                .andExpect(jsonPath("$.response.track").doesNotExist());
        verify(albums).select("album-1");
        verifyNoInteractions(tracks);
    }

    @Test
    void track_id는_track_경로로_전달한다() throws Exception {
        ImportedAlbum album = ImportedAlbum.of(5L, "album-1", "Album", 2024, List.of());
        ImportedTrack track = ImportedTrack.of(10L, "track-1", null, "Track", null,
                200_000, List.of(), album);
        when(tracks.select("track-1")).thenReturn(new TrackDetail(track, List.of()));

        mvc.perform(post("/api/catalog/import").contentType(APPLICATION_JSON)
                        .content("{\"subjectType\":\"TRACK\",\"spotifyId\":\"track-1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.response.subjectType").value("TRACK"))
                .andExpect(jsonPath("$.response.track.spotifyTrackId").value("track-1"))
                .andExpect(jsonPath("$.response.album").doesNotExist());
        verify(tracks).select("track-1");
        verifyNoInteractions(albums);
    }

    @Test
    void artist는_import를_거부한다() throws Exception {
        mvc.perform(post("/api/catalog/import").contentType(APPLICATION_JSON)
                        .content("{\"subjectType\":\"ARTIST\",\"spotifyId\":\"artist-1\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(tracks, albums);
    }
}
