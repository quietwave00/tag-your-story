package com.tagnote.application.catalog.search.port;

import java.util.List;

public interface SearchKeywordRankingReader {

    List<String> getTopSearchKeywordList();
}
