package com.tagstory.api.domain.board;

import com.tagstory.api.annotations.CurrentUserId;
import com.tagstory.api.domain.board.dto.request.CreateBoardRequest;
import com.tagstory.api.domain.board.dto.response.BoardByTrackResponse;
import com.tagstory.api.domain.board.dto.response.CreateBoardResponse;
import com.tagstory.api.domain.board.dto.response.DetailBoardResponse;
import com.tagstory.api.utils.ApiUtils;
import com.tagstory.api.utils.dto.ApiResult;
import com.tagstory.core.domain.board.dto.response.Board;
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
    public ApiResult<CreateBoardResponse> create(@RequestBody CreateBoardRequest request, @CurrentUserId Long userId) {
        Board response = boardFacade.create(request.toCommand(userId));
        return ApiUtils.success(CreateBoardResponse.from(response));
    }

    /*
     * 트랙 아이디에 해당하는 게시물을 조회한다.
     */
    @GetMapping("/boards/{trackId}")
    public ApiResult<List<BoardByTrackResponse>> getBoardListByTrackId(@PathVariable("trackId") String trackId, @RequestParam("page") int page) {
        List<Board> response = boardFacade.getBoardListByTrackId(trackId, page);
        return ApiUtils.success(response.stream().map(BoardByTrackResponse::from).collect(Collectors.toList()));
    }

    /*
     * 게시물을 조회한다.
     */
    @GetMapping("/boards")
    public ApiResult<DetailBoardResponse> getDetailBoard(@RequestParam("boardId") String boardId) {
        Board response = boardFacade.getDetailBoard(boardId);
        return ApiUtils.success(DetailBoardResponse.from(response));
    }
}
