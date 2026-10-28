package com.tagnote.application.catalog.search.port;

import com.tagnote.application.catalog.search.model.CatalogSearchWindow;

public interface CatalogSearchWindowStore {
    CatalogSearchWindow find(String keyword, int windowIndex);

    void save(String keyword, int windowIndex, CatalogSearchWindow window);
}
