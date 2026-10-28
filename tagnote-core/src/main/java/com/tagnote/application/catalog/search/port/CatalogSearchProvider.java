package com.tagnote.application.catalog.search.port;

import com.tagnote.application.catalog.search.model.CatalogSearchResult;
import com.tagnote.application.catalog.search.model.CatalogSearchScope;
import com.tagnote.application.catalog.search.model.CatalogSearchWindow;

public interface CatalogSearchProvider {

    CatalogSearchResult search(String keyword, int page, CatalogSearchScope scope);

    CatalogSearchWindow searchWindow(String keyword, int firstPage, int pageCount);
}
