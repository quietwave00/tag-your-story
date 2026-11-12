package com.tagnote.api.domain.catalog;

import com.tagnote.api.domain.catalog.dto.response.CatalogSubjectTagsResponse;
import com.tagnote.application.catalog.detail.CatalogSubjectTagsReadService;
import com.tagnote.core.utils.api.ApiResult;
import com.tagnote.core.utils.api.ApiUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/catalog")
@RequiredArgsConstructor
public class CatalogSubjectTagsController implements CatalogSubjectTagsApi {
    private final CatalogSubjectTagsReadService tags;

    @GetMapping("/tracks/{spotifyTrackId}/tags")
    @Override
    public ApiResult<CatalogSubjectTagsResponse> getTrackTags(
            @PathVariable("spotifyTrackId") String spotifyTrackId) {
        return ApiUtils.success(CatalogSubjectTagsResponse.from(tags.getTrackTags(spotifyTrackId)));
    }

    @GetMapping("/albums/{spotifyAlbumId}/tags")
    @Override
    public ApiResult<CatalogSubjectTagsResponse> getAlbumTags(
            @PathVariable("spotifyAlbumId") String spotifyAlbumId) {
        return ApiUtils.success(CatalogSubjectTagsResponse.from(tags.getAlbumTags(spotifyAlbumId)));
    }
}
