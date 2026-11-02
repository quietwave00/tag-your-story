package com.tagnote.api.domain.catalog;

import com.tagnote.api.domain.catalog.dto.response.CatalogAlbumDetailResponse;
import com.tagnote.api.domain.catalog.dto.response.CatalogAlbumTracksResponse;
import com.tagnote.api.domain.catalog.dto.response.CatalogArtistAlbumsResponse;
import com.tagnote.api.domain.catalog.dto.response.CatalogArtistDetailResponse;
import com.tagnote.application.catalog.detail.CatalogDetailService;
import com.tagnote.core.utils.api.ApiResult;
import com.tagnote.core.utils.api.ApiUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/catalog")
@RequiredArgsConstructor
public class CatalogDetailController implements CatalogDetailApi {
    private final CatalogDetailService catalogDetailService;
    private final CatalogDetailRequestValidator validator;

    @GetMapping("/albums/{spotifyAlbumId}")
    @Override
    public ApiResult<CatalogAlbumDetailResponse> getAlbum(@PathVariable("spotifyAlbumId") String spotifyAlbumId) {
        return ApiUtils.success(CatalogAlbumDetailResponse.from(catalogDetailService.getAlbum(spotifyAlbumId)));
    }

    @GetMapping("/artists/{spotifyArtistId}")
    @Override
    public ApiResult<CatalogArtistDetailResponse> getArtist(@PathVariable("spotifyArtistId") String spotifyArtistId) {
        return ApiUtils.success(CatalogArtistDetailResponse.from(catalogDetailService.getArtist(spotifyArtistId)));
    }

    @GetMapping("/artists/{spotifyArtistId}/albums")
    @Override
    public ResponseEntity<ApiResult<CatalogArtistAlbumsResponse>> getArtistAlbums(
            @PathVariable("spotifyArtistId") String spotifyArtistId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "releaseType", required = false) String releaseType) {
        return CatalogArtistAlbumsResponse.toHttpResponse(
                catalogDetailService.getArtistAlbumListing(spotifyArtistId,
                        validator.releaseType(releaseType), validator.page(page)));
    }

    @GetMapping("/albums/{spotifyAlbumId}/tracks")
    @Override
    public ApiResult<CatalogAlbumTracksResponse> getAlbumTracks(
            @PathVariable("spotifyAlbumId") String spotifyAlbumId,
            @RequestParam(value = "page", defaultValue = "0") int page) {
        return ApiUtils.success(CatalogAlbumTracksResponse.from(
                catalogDetailService.getAlbumTracks(spotifyAlbumId, validator.page(page))));
    }
}
