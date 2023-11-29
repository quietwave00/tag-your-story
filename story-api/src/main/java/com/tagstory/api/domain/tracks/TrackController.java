package com.tagstory.api.domain.tracks;

import com.tagstory.api.domain.tracks.dto.response.SearchTracksResponse;
import com.tagstory.core.domain.tracks.service.TrackService;
import com.tagstory.core.domain.tracks.service.dto.TrackData;
import com.tagstory.core.domain.tracks.service.dto.response.SearchTrackList;
import com.tagstory.core.utils.ApiUtils;
import com.tagstory.core.utils.dto.ApiResult;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class TrackController {

    private final TrackService trackService;

    /*
     * 트랙 검색
     */
    @GetMapping("/tracks")
    public ApiResult<SearchTracksResponse> search(@RequestParam("keyword") String keyword, @RequestParam("page") int page) {
        SearchTrackList searchTrackList = trackService.search(keyword, page);
        return ApiUtils.success(SearchTracksResponse.from(searchTrackList));
    }

    /*
     * 트랙 상세 조회
     */
    @GetMapping("/tracks/{trackId}")
    public ApiResult<TrackData> getDetail(@PathVariable("trackId") String trackId) {
        TrackData trackData = trackService.getDetail(trackId);
        return ApiUtils.success(trackData);
    }
}
