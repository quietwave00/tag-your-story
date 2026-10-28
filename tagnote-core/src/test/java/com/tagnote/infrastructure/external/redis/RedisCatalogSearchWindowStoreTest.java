package com.tagnote.infrastructure.external.redis;

import com.tagnote.application.catalog.search.model.CatalogSearchWindow;
import com.tagnote.core.common.CacheSpec;
import com.tagnote.core.common.CommonRedisTemplate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RedisCatalogSearchWindowStoreTest {
    @Mock private CommonRedisTemplate redis;

    @Test
    void 앨범_제목_필터_정책은_기존_검색_window와_다른_캐시_키를_사용한다() {
        RedisCatalogSearchWindowStore store = new RedisCatalogSearchWindowStore(redis);
        CatalogSearchWindow window = new CatalogSearchWindow(List.of(), List.of(), List.of(), 0, 0, 0);

        store.find("dreampop", 0);
        store.save("dreampop", 0, window);

        verify(redis).get("albumTitleV1:0:dreampop", CacheSpec.CATALOG_SEARCH_WINDOW);
        verify(redis).set("albumTitleV1:0:dreampop", window, CacheSpec.CATALOG_SEARCH_WINDOW);
    }
}
