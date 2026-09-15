package com.tagnote.application.catalog.search.model;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class CatalogSearchResult {

    private List<CatalogSearchItem> items;
    private long totalCount;

    public static CatalogSearchResult of(List<CatalogSearchItem> items, long totalCount) {
        return CatalogSearchResult.builder()
                .items(List.copyOf(items))
                .totalCount(totalCount)
                .build();
    }
}
