package com.tagnote.api.domain.catalog;

import com.tagnote.api.domain.catalog.dto.response.CatalogSearchResponse;
import com.tagnote.application.catalog.search.CatalogSearchService;
import com.tagnote.application.catalog.search.CatalogScreenSearchService;
import com.tagnote.application.catalog.search.model.CatalogSearchResult;
import com.tagnote.application.catalog.search.model.CatalogSearchScope;
import com.tagnote.application.catalog.search.model.CatalogSearchSubjectType;
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
@RequestMapping("/api/catalog")
@RequiredArgsConstructor
public class CatalogSearchController implements CatalogSearchApi {

    private final CatalogSearchService catalogSearchService;

    @GetMapping("/search")
    @Override
    public ApiResult<CatalogSearchResponse> search(
            @RequestParam("keyword") String keyword,
            @RequestParam("page") int page,
            @RequestParam(value = "type", required = false) String type
    ) {
        CatalogSearchScope scope = searchScope(type);
        if (scope != CatalogSearchScope.ALL && (page < 0 || page > CatalogScreenSearchService.MAX_PAGE)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "page must be between 0 and 100");
        }
        CatalogSearchResult result = catalogSearchService.search(keyword, page, scope);
        return ApiUtils.success(CatalogSearchResponse.from(result));
    }

    private CatalogSearchScope searchScope(String type) {
        if (type == null) return CatalogSearchScope.ALL;
        try {
            return CatalogSearchScope.valueOf(CatalogSearchSubjectType.valueOf(type).name());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "type must be TRACK, ALBUM or ARTIST");
        }
    }
}
