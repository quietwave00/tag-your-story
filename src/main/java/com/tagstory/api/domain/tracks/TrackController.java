package com.tagstory.api.domain.tracks;

import com.tagstory.api.utils.ApiUtils;
import com.tagstory.api.utils.dto.ApiResult;
import com.tagstory.core.domain.tracks.service.TrackService;
import com.tagstory.core.domain.tracks.service.dto.response.SearchTracksResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
        List<SearchTracksResponse> searchTracksResponseList = trackService.search(keyword, page);
        return ApiUtils.success(searchTracksResponseList);
    }

}
