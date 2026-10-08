package com.tagnote.application.catalog.search;

import com.tagnote.application.catalog.search.model.CatalogSearchItem;
import com.tagnote.application.catalog.search.model.CatalogSearchResult;
import com.tagnote.application.catalog.search.port.CatalogSearchProvider;
import com.tagnote.application.catalog.search.port.SearchKeywordRecorder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CatalogSearchService {

    private final SearchKeywordRecorder searchKeywordRecorder;
    private final CatalogSearchProvider catalogSearchProvider;

    public CatalogSearchResult search(String keyword, int page) {
        searchKeywordRecorder.record(keyword);
        CatalogSearchResult providerResult = catalogSearchProvider.search(keyword, page);
        List<CatalogSearchItem> rankedItems = CatalogSearchRanker.rank(keyword, providerResult.getItems());
        return CatalogSearchResult.of(rankedItems, providerResult.getTotalCount());
    }
}
