package com.tagnote.application.catalog.search.model;

import java.util.List;

public record CatalogScreenSearchResult(Section tracks, Section albums, Section artists) {
    public record Section(List<CatalogSearchItem> items, long totalCount, boolean hasNext) {
        public Section {
            items = List.copyOf(items);
        }
    }
}
