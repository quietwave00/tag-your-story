package com.tagstory.api.domain.tracks;

import com.tagstory.api.domain.tracks.dto.response.DetailTrackResponse;
import com.tagstory.api.domain.tracks.dto.response.SearchTracksResponse;
import com.tagstory.api.utils.ApiUtils;
import com.tagstory.api.utils.dto.ApiResult;
import com.tagstory.core.domain.tracks.service.TrackService;
import com.tagstory.core.domain.tracks.service.dto.response.DetailTrack;
import com.tagstory.core.domain.tracks.service.dto.response.SearchTracks;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class TrackController {

    private final TrackService trackService;

    /*
     * 트랙 검색
     */
    @GetMapping("/tracks")
    public ApiResult<List<SearchTracksResponse>> search(@RequestParam("keyword") String keyword, @RequestParam("page") int page) {
        List<SearchTracks> searchTracksList = trackService.search(keyword, page);
        return ApiUtils.success(searchTracksList.stream().map(SearchTracksResponse::create).collect(Collectors.toList()));
    }

    /*
     * 트랙 상세 조회
     */
    @GetMapping("/tracks/{trackId}")
    public ApiResult<DetailTrackResponse> getDetail(@PathVariable("trackId") String trackId) {
        DetailTrack detailTrack = trackService.getDetail(trackId);
        return ApiUtils.success(DetailTrackResponse.create(detailTrack));
    }
}
