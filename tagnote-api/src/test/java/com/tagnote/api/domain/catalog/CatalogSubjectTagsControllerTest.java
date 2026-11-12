package com.tagnote.api.domain.catalog;

import com.tagnote.api.support.WebMvcMethodSecurityTestConfig;
import com.tagnote.application.catalog.detail.CatalogSubjectTagsReadService;
import com.tagnote.application.catalog.detail.model.PreviewTagDetail;
import com.tagnote.application.catalog.detail.model.SubjectTags;
import com.tagnote.application.catalog.detail.model.SystemTagDetail;
import com.tagnote.core.exception.CustomException;
import com.tagnote.core.exception.ExceptionCode;
import com.tagnote.domain.enrichment.observation.ExternalTagSource;
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

@WebMvcTest(CatalogSubjectTagsController.class)
@Import(WebMvcMethodSecurityTestConfig.class)
class CatalogSubjectTagsControllerTest {
    @Autowired
    private MockMvc mvc;

    @MockBean
    private CatalogSubjectTagsReadService tags;

    @Test
    void 트랙_태그는_저장된_확정_태그만_반환한다() throws Exception {
        when(tags.getTrackTags("track-1")).thenReturn(SubjectTags.confirmed(
                List.of(new SystemTagDetail(11L, "Rock", 0.9))));

        mvc.perform(get("/api/catalog/tracks/track-1/tags"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.response.systemTags[0].tagId").value(11))
                .andExpect(jsonPath("$.response.systemTags[0].name").value("Rock"))
                .andExpect(jsonPath("$.response.systemTags[0].score").value(0.9))
                .andExpect(jsonPath("$.response.previewTags").isEmpty())
                .andExpect(jsonPath("$.response.tagDisplayStatus").value("CONFIRMED"))
                .andExpect(jsonPath("$.response.album").doesNotExist())
                .andExpect(jsonPath("$.response.track").doesNotExist());
        verify(tags).getTrackTags("track-1");
    }

    @Test
    void 앨범_태그는_저장된_미리보기만_반환한다() throws Exception {
        when(tags.getAlbumTags("album-1")).thenReturn(SubjectTags.preview(
                List.of(new PreviewTagDetail("Dream Pop", ExternalTagSource.LASTFM))));

        mvc.perform(get("/api/catalog/albums/album-1/tags"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.response.systemTags").isEmpty())
                .andExpect(jsonPath("$.response.previewTags[0].name").value("Dream Pop"))
                .andExpect(jsonPath("$.response.previewTags[0].source").value("LASTFM"))
                .andExpect(jsonPath("$.response.tagDisplayStatus").value("PREVIEW"))
                .andExpect(jsonPath("$.response.album").doesNotExist())
                .andExpect(jsonPath("$.response.track").doesNotExist());
        verify(tags).getAlbumTags("album-1");
    }

    @Test
    void 확정과_미리보기가_없으면_EMPTY와_빈_배열을_반환한다() throws Exception {
        when(tags.getTrackTags("track-1")).thenReturn(SubjectTags.confirmed(List.of()));

        mvc.perform(get("/api/catalog/tracks/track-1/tags"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.response.systemTags").isEmpty())
                .andExpect(jsonPath("$.response.previewTags").isEmpty())
                .andExpect(jsonPath("$.response.tagDisplayStatus").value("EMPTY"));
    }

    @Test
    void import_전_앨범이_없으면_기존_오류_응답을_유지한다() throws Exception {
        when(tags.getAlbumTags("unavailable"))
                .thenThrow(new CustomException(ExceptionCode.ALBUM_NOT_FOUND));

        mvc.perform(get("/api/catalog/albums/unavailable/tags"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.response.exceptionCode").value("ALBUM_NOT_FOUND"))
                .andExpect(jsonPath("$.response.status").value(404));
    }
}
