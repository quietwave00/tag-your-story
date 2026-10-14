package com.tagnote.api.domain.board;

import com.tagnote.api.annotations.CurrentUserId;
import com.tagnote.api.domain.board.dto.request.CreateBoardRequest;
import com.tagnote.api.domain.board.dto.request.UpdateBoardRequest;
import com.tagnote.api.domain.board.dto.response.*;
import com.tagnote.application.board.BoardCommandService;
import com.tagnote.application.board.BoardQueryService;
import com.tagnote.application.board.model.BoardPage;
import com.tagnote.application.board.model.BoardView;
import com.tagnote.core.domain.board.BoardOrderType;
import com.tagnote.core.utils.api.ApiResult;
import com.tagnote.core.utils.api.ApiUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/boards")
public class BoardController implements BoardApi {

    private final BoardCommandService commandService;
    private final BoardQueryService queryService;

    /*
     * 게시글을 작성한다.
     */
    @PreAuthorize("hasRole('USER')")
    @PostMapping
    @Override
    public ApiResult<CreateBoardResponse> create(@RequestBody @Valid CreateBoardRequest request, @CurrentUserId Long userId) {
        BoardView response = commandService.create(request.toCommand(userId));
        return ApiUtils.success(CreateBoardResponse.from(response));
    }

    /*
     * 트랙 아이디에 해당하는 게시물 리스트를 조회한다.
     */
    @GetMapping("/{trackId}")
    @Override
    public ApiResult<BoardListResponse> getBoardListByTrackId(@PathVariable("trackId") String trackId,
                                                              @RequestParam("order-type") BoardOrderType orderType,
                                                              @RequestParam("page") int page) {
        BoardPage boardList = queryService.getTrackBoards(trackId, orderType, page);
        return ApiUtils.success(BoardListResponse.from(boardList));
    }

    @GetMapping("/albums/{albumId}")
    @Override
    public ApiResult<BoardListResponse> getBoardListByAlbumId(
            @PathVariable("albumId") String albumId,
            @RequestParam("order-type") BoardOrderType orderType,
            @RequestParam("page") int page
    ) {
        return ApiUtils.success(BoardListResponse.from(queryService.getAlbumBoards(albumId, orderType, page)));
    }

    /*
     * 게시물을 조회한다.
     */
    @GetMapping
    @Override
    public ApiResult<DetailBoardResponse> getDetailBoard(@RequestParam("boardId") String boardId) {
        BoardView response = queryService.getDetail(boardId);
        return ApiUtils.success(DetailBoardResponse.from(response));
    }

    /*
     * 트랙 아이디에 대한 전체 게시물 개수를 조회한다.
     */
    @GetMapping("/count/{trackId}")
    @Override
    public ApiResult<BoardCountResponse> getBoardCountByTrackId(@PathVariable("trackId") String trackId) {
        int response = queryService.countTrackBoards(trackId);
        return ApiUtils.success(BoardCountResponse.from(response));
    }

    @GetMapping("/count/albums/{albumId}")
    @Override
    public ApiResult<BoardCountResponse> getBoardCountByAlbumId(@PathVariable("albumId") String albumId) {
        return ApiUtils.success(BoardCountResponse.from(queryService.countAlbumBoards(albumId)));
    }


    /*
     * 유저 태그 이름을 갖고 있는 게시물 목록을 조회한다.
     */
    @GetMapping("/user-tags")
    @Override
    public ApiResult<List<BoardResponse>> getBoardListByUserTagName(@RequestParam("userTagName") String userTagName) {
        List<BoardView> boardList = queryService.getBoardsByUserTagName(userTagName);
        return ApiUtils.success(boardList.stream().map(BoardResponse::from).toList());
    }

    /*
     * 게시글에 대한 권한을 확인한다.
     */
    @PreAuthorize("hasRole('USER')")
    @GetMapping("/auth/{boardId}")
    @Override
    public ApiResult<Boolean> isWriter(@PathVariable("boardId") String boardId, @CurrentUserId Long userId) {
        Boolean isWriter = queryService.isWriter(boardId, userId);
        return ApiUtils.success(isWriter);
    }

    /*
     * 게시글을 수정한다.
     */
    @PreAuthorize("hasRole('USER')")
    @PatchMapping
    @Override
    public ApiResult<BoardResponse> updateBoardAndUserTag(
            @RequestBody @Valid UpdateBoardRequest request,
            @CurrentUserId Long userId
    ) {
        BoardView board = commandService.update(request.toCommand(), userId);
        return ApiUtils.success(BoardResponse.from(board));
    }

    /*
     * 게시글을 삭제한다.
     */
    @PreAuthorize("hasRole('USER')")
    @DeleteMapping("/{boardId}")
    @Override
    public ApiResult<Void> delete(@PathVariable("boardId") String boardId, @CurrentUserId Long userId) {
        commandService.delete(boardId, userId);
        return ApiUtils.success();
    }
}
