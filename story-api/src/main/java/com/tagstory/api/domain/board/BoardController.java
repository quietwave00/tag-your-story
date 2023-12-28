package com.tagstory.api.domain.board;

import com.tagstory.api.annotations.CurrentUserId;
import com.tagstory.api.domain.board.dto.request.CreateBoardRequest;
import com.tagstory.api.domain.board.dto.request.UpdateBoardRequest;
import com.tagstory.api.domain.board.dto.response.BoardCountResponse;
import com.tagstory.api.domain.board.dto.response.BoardResponse;
import com.tagstory.api.domain.board.dto.response.CreateBoardResponse;
import com.tagstory.api.domain.board.dto.response.DetailBoardResponse;
import com.tagstory.core.domain.board.BoardOrderType;
import com.tagstory.core.domain.board.dto.response.Board;
import com.tagstory.core.domain.board.service.BoardFacade;
import com.tagstory.core.utils.api.ApiResult;
import com.tagstory.core.utils.api.ApiUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/boards")
public class BoardController {

    private final BoardFacade boardFacade;

    /*
     * 게시글을 작성한다.
     */
    @PreAuthorize("hasRole('USER')")
    @PostMapping
    public ApiResult<CreateBoardResponse> create(@RequestBody @Valid CreateBoardRequest request, @CurrentUserId Long userId) {
        Board response = boardFacade.create(request.toCommand(userId));
        return ApiUtils.success(CreateBoardResponse.from(response));
    }

    /*
     * 트랙 아이디에 해당하는 게시물 리스트를 조회한다.
     */
    @GetMapping("/{trackId}")
    public ApiResult<List<BoardResponse>> getBoardListByTrackId(@PathVariable("trackId") String trackId,
                                                                @RequestParam("order-type") BoardOrderType orderType,
                                                                @RequestParam("page") int page) {
        List<Board> response = boardFacade.getBoardListByTrackId(trackId, orderType, page);
        return ApiUtils.success(response.stream().map(BoardResponse::from).toList());
    }

    /*
     * 게시물을 조회한다.
     */
    @GetMapping
    public ApiResult<DetailBoardResponse> getDetailBoard(@RequestParam("boardId") String boardId) {
        Board response = boardFacade.getDetailBoard(boardId);
        return ApiUtils.success(DetailBoardResponse.from(response));
    }

    /*
     * 트랙 아이디에 대한 전체 게시물 개수를 조회한다.
     */
    @GetMapping("/count/{trackId}")
    public ApiResult<BoardCountResponse> getBoardCountByTrackId(@PathVariable("trackId") String trackId) {
        int response = boardFacade.getBoardCountByTrackId(trackId);
        return ApiUtils.success(BoardCountResponse.from(response));
    }


    /*
     * 해시태그 이름을 갖고 있는 게시물 목록을 조회한다.
     */
    @GetMapping("/hashtags")
    public ApiResult<List<BoardResponse>> getBoardListByHashtagName(@RequestParam("hashtagName") String hashtagName) {
        List<Board> response = boardFacade.getBoardListByHashtagName(hashtagName);
        return ApiUtils.success(response.stream().map(BoardResponse::from).toList());
    }

    /*
     * 게시글에 대한 권한을 확인한다.
     */
    @PreAuthorize("hasRole('USER')")
    @GetMapping("/auth/{boardId}")
    public ApiResult<Boolean> isWriter(@PathVariable("boardId") String boardId, @CurrentUserId Long userId) {
        Boolean isWriter = boardFacade.isWriter(boardId, userId);
        return ApiUtils.success(isWriter);
    }

    /*
     * 게시글을 수정한다.
     */
    @PreAuthorize("hasRole('USER')")
    @PatchMapping
    public ApiResult<BoardResponse> updateBoardAndHashtag(@RequestBody @Valid UpdateBoardRequest request) {
        Board board = boardFacade.updateBoardAndHashtag(request.toCommand());
        return ApiUtils.success(BoardResponse.from(board));
    }

    /*
     * 게시글을 삭제한다.
     */
    @PreAuthorize("hasRole('USER')")
    @DeleteMapping("/{boardId}")
    public ApiResult<Void> delete(@PathVariable("boardId") String boardId) {
        boardFacade.delete(boardId);
        return ApiUtils.success();
    }
}
