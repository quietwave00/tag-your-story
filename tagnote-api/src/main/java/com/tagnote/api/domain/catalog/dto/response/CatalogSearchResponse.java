package com.tagnote.api.domain.catalog.dto.response;

import com.tagnote.application.catalog.search.model.CatalogSearchResult;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class CatalogSearchResponse {

    @Schema(description = "현재 page의 통합 정렬된 검색 후보. 최대 30개", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<CatalogSearchItemResponse> items;

    @Schema(description = "Spotify가 반환한 Track, Album, Artist total의 합", example = "153", requiredMode = Schema.RequiredMode.REQUIRED)
    private long totalCount;

    public static CatalogSearchResponse from(CatalogSearchResult result) {
        return CatalogSearchResponse.builder()
                .items(result.getItems().stream().map(CatalogSearchItemResponse::from).toList())
                .totalCount(result.getTotalCount())
                .build();
    }
}
