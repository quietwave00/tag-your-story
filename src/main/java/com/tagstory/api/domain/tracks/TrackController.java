package com.tagstory.api.domain.tracks;

import com.tagstory.api.utils.ApiUtils;
import com.tagstory.api.utils.dto.ApiResult;
import com.tagstory.core.domain.tracks.service.TrackService;
import com.tagstory.core.domain.tracks.service.dto.response.SearchTracksResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class TrackController {

    private final TrackService trackService;

    /*
     * 트랙 검색
     */
    @GetMapping("/tracks/{keyword}/{page}")
    public ApiResult<List<SearchTracksResponse>> search(@PathVariable("keyword") String keyword, @PathVariable("page") int page) {
        List<SearchTracksResponse> searchTracksResponseList = trackService.search(keyword, page);
        return ApiUtils.success(searchTracksResponseList);
    }

}
