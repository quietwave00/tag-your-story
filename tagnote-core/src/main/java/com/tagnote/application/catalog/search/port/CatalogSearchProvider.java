package com.tagnote.application.catalog.search.port;

import com.tagnote.application.catalog.search.model.CatalogSearchResult;

public interface CatalogSearchProvider {

    CatalogSearchResult search(String keyword, int page);
}
