package com.tagnote.infrastructure.external.redis;

import com.tagnote.application.catalog.search.model.CatalogSearchWindow;
import com.tagnote.application.catalog.search.port.CatalogSearchWindowStore;
import com.tagnote.core.common.CacheSpec;
import com.tagnote.core.common.CommonRedisTemplate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class RedisCatalogSearchWindowStore implements CatalogSearchWindowStore {
    private final CommonRedisTemplate redis;

    @Override
    public CatalogSearchWindow find(String keyword, int windowIndex) {
        return redis.get(key(keyword, windowIndex), CacheSpec.CATALOG_SEARCH_WINDOW);
    }

    @Override
    public void save(String keyword, int windowIndex, CatalogSearchWindow window) {
        redis.set(key(keyword, windowIndex), window, CacheSpec.CATALOG_SEARCH_WINDOW);
    }

    private String key(String keyword, int windowIndex) {
        return "albumTitleV1:" + windowIndex + ":" + keyword;
    }
}
