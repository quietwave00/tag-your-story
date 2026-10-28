package com.tagnote.api.domain.catalog;

import com.tagnote.api.domain.catalog.dto.response.CatalogScreenSearchResponse;
import com.tagnote.application.catalog.search.CatalogScreenSearchService;
import com.tagnote.core.utils.api.ApiResult;
import com.tagnote.core.utils.api.ApiUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/catalog/search")
@RequiredArgsConstructor
public class CatalogScreenSearchController implements CatalogScreenSearchApi {
    private final CatalogScreenSearchService searchService;

    @GetMapping("/screen")
    @Override
    public ApiResult<CatalogScreenSearchResponse> searchScreen(
            @RequestParam("keyword") String keyword,
            @RequestParam(value = "trackPage", defaultValue = "0") int trackPage,
            @RequestParam(value = "albumPage", defaultValue = "0") int albumPage,
            @RequestParam(value = "artistPage", defaultValue = "0") int artistPage) {
        if (invalid(trackPage) || invalid(albumPage) || invalid(artistPage)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "page must be between 0 and 100");
        }
        return ApiUtils.success(CatalogScreenSearchResponse.from(
                searchService.search(keyword, trackPage, albumPage, artistPage)));
    }

    private boolean invalid(int page) {
        return page < 0 || page > CatalogScreenSearchService.MAX_PAGE;
    }
}
