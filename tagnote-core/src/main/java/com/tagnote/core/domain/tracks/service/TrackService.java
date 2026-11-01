package com.tagnote.core.domain.tracks.service;

import com.tagnote.application.catalog.search.port.SearchKeywordRankingReader;
import com.tagnote.application.catalog.detail.port.TrackDetailProvider;
import com.tagnote.core.domain.tracks.service.dto.TrackData;
import com.tagnote.core.domain.tracks.service.dto.response.RankingList;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class TrackService {

    private final TrackDetailProvider trackDetailProvider;
    private final SearchKeywordRankingReader searchKeywordRankingReader;

    public TrackData getDetail(String trackId) {
        return trackDetailProvider.getTrack(trackId);
    }

    public RankingList getKeywordRanking() {
        return RankingList.onComplete(searchKeywordRankingReader.getTopSearchKeywordList());
    }

}
