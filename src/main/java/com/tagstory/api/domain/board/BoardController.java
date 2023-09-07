package com.tagstory.api.domain.board;

import com.tagstory.api.annotations.CurrentUserId;
import com.tagstory.api.domain.board.dto.request.CreateBoardRequest;
import com.tagstory.api.domain.board.dto.response.BoardByTrackResponse;
import com.tagstory.api.domain.board.dto.response.CreateBoardResponse;
import com.tagstory.api.utils.ApiUtils;
import com.tagstory.api.utils.dto.ApiResult;
import com.tagstory.core.domain.board.dto.response.BoardByTrack;
import com.tagstory.core.domain.board.dto.response.CreateBoard;
import com.tagstory.core.domain.board.service.BoardFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class BoardController {

    private final BoardFacade boardFacade;

    /*
     * 게시글을 작성한다.
     */
    @PreAuthorize("hasRole('ROLE_USER')")
    @PostMapping("/boards")
    public ApiResult<CreateBoardResponse> create(@RequestBody CreateBoardRequest createBoardRequest, @CurrentUserId Long userId) {
        CreateBoard createBoard = boardFacade.create(createBoardRequest.toCommand(userId));
        return ApiUtils.success(CreateBoardResponse.create(createBoard));
    }

    /*
     * 트랙 아이디에 해당하는 모든 게시물을 조회한다.
     */
    @GetMapping("/boards/{trackId}")
    public ApiResult<List<BoardByTrackResponse>> getBoardListByTrackId(@PathVariable("trackId") String trackId) {
        List<BoardByTrack> boardByTrack = boardFacade.getBoardListByTrackId(trackId);
        return ApiUtils.success(boardByTrack.stream().map(BoardByTrackResponse::create).collect(Collectors.toList()));
    }
}
