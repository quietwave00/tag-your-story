package com.tagnote.api.domain.boardusertag;

import com.tagnote.api.domain.boardusertag.dto.response.BoardUserTagResponse;
import com.tagnote.core.domain.boardusertag.service.BoardUserTag;
import com.tagnote.core.domain.boardusertag.service.BoardUserTagService;
import com.tagnote.core.utils.api.ApiResult;
import com.tagnote.core.utils.api.ApiUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/board-user-tags")
public class BoardUserTagController {
    private final BoardUserTagService boardUserTagService;

    @GetMapping("/recent")
    public ApiResult<List<BoardUserTagResponse>> getRecentUserTagList() {
        List<BoardUserTag> boardUserTagList = boardUserTagService.getRecentBoardUserTagList();
        return ApiUtils.success(boardUserTagList.stream().map(BoardUserTagResponse::from).toList());
    }
}
