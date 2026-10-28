package com.tagnote.application.catalog.detail.model;

import java.util.List;

public record SpotifyCatalogPage<T>(List<T> items, long totalCount) {
    public SpotifyCatalogPage {
        items = List.copyOf(items);
    }
}
