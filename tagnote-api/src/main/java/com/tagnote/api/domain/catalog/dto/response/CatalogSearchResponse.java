package com.tagnote.api.domain.catalog.dto.response;

import com.tagnote.application.catalog.search.model.CatalogSearchResult;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class CatalogSearchResponse {

    @Schema(description = "현재 page 검색 후보. 통합 검색은 최대 30개, 타입별 검색은 최대 10개", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<CatalogSearchItemResponse> items;

    @Schema(description = "통합 검색은 세 타입 total 합계, 타입별 검색은 선택 타입의 Spotify total", example = "153", requiredMode = Schema.RequiredMode.REQUIRED)
    private long totalCount;

    public static CatalogSearchResponse from(CatalogSearchResult result) {
        return CatalogSearchResponse.builder()
                .items(result.getItems().stream().map(CatalogSearchItemResponse::from).toList())
                .totalCount(result.getTotalCount())
                .build();
    }
}
