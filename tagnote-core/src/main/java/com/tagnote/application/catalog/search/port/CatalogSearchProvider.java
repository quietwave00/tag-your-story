package com.tagnote.application.catalog.search.port;

import com.tagnote.application.catalog.search.model.CatalogSearchResult;
import com.tagnote.application.catalog.search.model.CatalogSearchScope;

public interface CatalogSearchProvider {

    CatalogSearchResult search(String keyword, int page, CatalogSearchScope scope);
}
