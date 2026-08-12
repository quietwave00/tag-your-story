package com.tagnote.application.catalog.search;

import com.tagnote.application.catalog.search.model.TrackSearchResult;
import com.tagnote.application.catalog.search.port.SearchKeywordRecorder;
import com.tagnote.application.catalog.search.port.TrackSearchProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TrackSearchService {

    private final SearchKeywordRecorder searchKeywordRecorder;
    private final TrackSearchProvider trackSearchProvider;

    public TrackSearchResult search(String keyword, int page) {
        searchKeywordRecorder.record(keyword);
        return trackSearchProvider.search(keyword, page);
    }
}
