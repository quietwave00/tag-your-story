package com.tagnote.application.catalog.search.model;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class TrackSearchResult {

    private List<TrackSearchItem> items;
    private Integer totalCount;

    public static TrackSearchResult of(List<TrackSearchItem> items, Integer totalCount) {
        return TrackSearchResult.builder()
                .items(items)
                .totalCount(totalCount)
                .build();
    }
}
