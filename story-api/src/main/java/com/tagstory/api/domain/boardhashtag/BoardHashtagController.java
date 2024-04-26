package com.tagstory.api.domain.boardhashtag;

import com.tagstory.api.domain.boardhashtag.dto.response.BoardHashtagResponse;
import com.tagstory.core.domain.boardhashtag.service.BoardHashtag;
import com.tagstory.core.domain.boardhashtag.service.BoardHashtagService;
import com.tagstory.core.utils.api.ApiResult;
import com.tagstory.core.utils.api.ApiUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/boardhashtags")
public class BoardHashtagController {
    private final BoardHashtagService boardHashtagService;

    @GetMapping("/recent")
    public ApiResult<List<BoardHashtagResponse>> getRecentHashtagList() {
        List<BoardHashtag> boardHashtagList = boardHashtagService.getRecentBoardHashtagList();
        return ApiUtils.success(boardHashtagList.stream().map(BoardHashtagResponse::from).toList());
    }
}
