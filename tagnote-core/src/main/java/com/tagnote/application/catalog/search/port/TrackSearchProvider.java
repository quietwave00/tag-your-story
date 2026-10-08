package com.tagnote.application.catalog.search.port;

import com.tagnote.application.catalog.search.model.TrackSearchResult;

public interface TrackSearchProvider {

    TrackSearchResult search(String keyword, int page);
}
