package com.tagnote.api.domain.tracks;

import com.tagnote.api.domain.tracks.dto.response.RankingListResponse;
import com.tagnote.api.domain.tracks.dto.response.SearchTracksResponse;
import com.tagnote.application.catalog.search.TrackSearchService;
import com.tagnote.application.catalog.search.model.TrackSearchResult;
import com.tagnote.core.domain.tracks.service.TrackService;
import com.tagnote.core.domain.tracks.service.dto.TrackData;
import com.tagnote.core.domain.tracks.service.dto.response.RankingList;
import com.tagnote.core.utils.api.ApiUtils;
import com.tagnote.core.utils.api.ApiResult;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class TrackController implements TrackApi {

    private final TrackSearchService trackSearchService;
    private final TrackService trackService;

    /*
     * 트랙을 검색한다.
     */
    @GetMapping("/tracks")
    @Override
    public ApiResult<SearchTracksResponse> search(@RequestParam("keyword") String keyword, @RequestParam("page") int page) {
        TrackSearchResult searchResult = trackSearchService.search(keyword, page);
        return ApiUtils.success(SearchTracksResponse.from(searchResult));
    }

    /*
     * 트랙을 상세 조회한다.
     */
    @GetMapping("/tracks/{trackId}")
    @Override
    public ApiResult<TrackData> getDetail(@PathVariable("trackId") String trackId) {
        TrackData trackData = trackService.getDetail(trackId);
        return ApiUtils.success(trackData);
    }

    /*
     * 검색어 순위를 돌려준다.
     */
    @GetMapping("/tracks/ranking")
    @Override
    public ApiResult<RankingListResponse> getKeywordRanking() {
        RankingList rankingList = trackService.getKeywordRanking();
        return ApiUtils.success(RankingListResponse.from(rankingList));
    }
}
