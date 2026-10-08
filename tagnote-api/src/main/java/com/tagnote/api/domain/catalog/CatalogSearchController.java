package com.tagnote.api.domain.catalog;

import com.tagnote.api.domain.catalog.dto.response.CatalogSearchResponse;
import com.tagnote.application.catalog.search.CatalogSearchService;
import com.tagnote.application.catalog.search.model.CatalogSearchResult;
import com.tagnote.core.utils.api.ApiResult;
import com.tagnote.core.utils.api.ApiUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/catalog")
@RequiredArgsConstructor
public class CatalogSearchController implements CatalogSearchApi {

    private final CatalogSearchService catalogSearchService;

    @GetMapping("/search")
    @Override
    public ApiResult<CatalogSearchResponse> search(
            @RequestParam("keyword") String keyword,
            @RequestParam("page") int page
    ) {
        CatalogSearchResult result = catalogSearchService.search(keyword, page);
        return ApiUtils.success(CatalogSearchResponse.from(result));
    }
}
