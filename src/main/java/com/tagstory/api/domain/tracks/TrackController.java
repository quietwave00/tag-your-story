package com.tagstory.api.domain.tracks;

import com.tagstory.api.domain.tracks.dto.response.DetailTrack;
import com.tagstory.api.domain.tracks.dto.response.SearchTracks;
import com.tagstory.api.utils.ApiUtils;
import com.tagstory.api.utils.dto.ApiResult;
import com.tagstory.core.domain.tracks.service.TrackService;
import com.tagstory.core.domain.tracks.service.dto.response.DetailTrackResponse;
import com.tagstory.core.domain.tracks.service.dto.response.SearchTracksResponse;
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
    public ApiResult<List<SearchTracks>> search(@RequestParam("keyword") String keyword, @RequestParam("page") int page) {
        List<SearchTracksResponse> searchTracksResponseList = trackService.search(keyword, page);
        return ApiUtils.success(searchTracksResponseList.stream().map(SearchTracks::create).collect(Collectors.toList()));
    }

    /*
     * 트랙 상세 조회
     */
    @GetMapping("/tracks/{trackId}")
    public ApiResult<DetailTrack> getDetail(@PathVariable("trackId") String trackId) {
        DetailTrackResponse detailTrackResponse = trackService.getDetail(trackId);
        return ApiUtils.success(DetailTrack.create(detailTrackResponse));
    }
}
