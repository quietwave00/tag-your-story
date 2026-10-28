package com.tagnote.api.domain.catalog;

import com.tagnote.api.domain.catalog.dto.response.CatalogAlbumDetailResponse;
import com.tagnote.api.domain.catalog.dto.response.CatalogAlbumTracksResponse;
import com.tagnote.api.domain.catalog.dto.response.CatalogArtistAlbumsResponse;
import com.tagnote.api.domain.catalog.dto.response.CatalogArtistDetailResponse;
import com.tagnote.application.catalog.detail.CatalogDetailService;
import com.tagnote.core.utils.api.ApiResult;
import com.tagnote.core.utils.api.ApiUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/catalog")
@RequiredArgsConstructor
public class CatalogDetailController implements CatalogDetailApi {
    private static final int PAGE_SIZE = 10;

    private final CatalogDetailService catalogDetailService;

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
    public ApiResult<CatalogArtistAlbumsResponse> getArtistAlbums(
            @PathVariable("spotifyArtistId") String spotifyArtistId,
            @RequestParam(value = "page", defaultValue = "0") int page) {
        return ApiUtils.success(CatalogArtistAlbumsResponse.from(
                catalogDetailService.getArtistAlbums(spotifyArtistId, validPage(page))));
    }

    @GetMapping("/albums/{spotifyAlbumId}/tracks")
    @Override
    public ApiResult<CatalogAlbumTracksResponse> getAlbumTracks(
            @PathVariable("spotifyAlbumId") String spotifyAlbumId,
            @RequestParam(value = "page", defaultValue = "0") int page) {
        return ApiUtils.success(CatalogAlbumTracksResponse.from(
                catalogDetailService.getAlbumTracks(spotifyAlbumId, validPage(page))));
    }

    private int validPage(int page) {
        if (page < 0 || page > Integer.MAX_VALUE / PAGE_SIZE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "page must be a non-negative 0-based index");
        }
        return page;
    }
}
