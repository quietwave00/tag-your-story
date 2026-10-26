package com.tagnote.api.domain.like;

import com.tagnote.api.annotations.CurrentUserId;
import com.tagnote.api.domain.like.dto.request.UnLikeRequest;
import com.tagnote.api.domain.like.dto.request.LikeBoardRequest;
import com.tagnote.api.domain.like.dto.response.LikeStatusResponse;
import com.tagnote.api.domain.like.dto.response.LikedBoardResponse;
import com.tagnote.application.like.LikeCommandService;
import com.tagnote.application.like.LikeQueryService;
import com.tagnote.core.utils.api.ApiUtils;
import com.tagnote.core.utils.api.ApiResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class LikeController implements LikeApi {
    private final LikeCommandService commandService;
    private final LikeQueryService queryService;

    /*
     * 게시글에 좋아요를 등록한다.
     */
    @PreAuthorize("hasRole('USER')")
    @PostMapping("/likes")
    public ApiResult<Boolean> like(@RequestBody @Valid LikeBoardRequest likeBoardRequest, @CurrentUserId Long userId) {
        Boolean result = commandService.like(likeBoardRequest.getBoardId(), userId);
        return ApiUtils.success(result);
    }

    /*
     * 게시글에 좋아요를 취소한다.
     */
    @PreAuthorize("hasRole('USER')")
    @DeleteMapping("/likes")
    public ApiResult<Void> unLike(@RequestBody @Valid UnLikeRequest request, @CurrentUserId Long userId) {
        commandService.unLike(request.getBoardId(), userId);
        return ApiUtils.success();
    }

    /*
     * 사용자의 좋아요 여부를 체크한다.
     */
    @PreAuthorize("hasRole('USER')")
    @GetMapping("/likes/status/{boardId}")
    public ApiResult<LikeStatusResponse> isLiked(@PathVariable("boardId") String boardId, @CurrentUserId Long userId) {
        boolean isLiked = queryService.isLiked(boardId, userId);
        return ApiUtils.success(LikeStatusResponse.from(isLiked));
    }

    @PreAuthorize("hasRole('USER')")
    @PostMapping("/boards/{boardId}/likes")
    public ApiResult<Boolean> likeBoard(@PathVariable("boardId") String boardId, @CurrentUserId Long userId) {
        return ApiUtils.success(commandService.like(boardId, userId));
    }

    @PreAuthorize("hasRole('USER')")
    @DeleteMapping("/boards/{boardId}/likes")
    public ApiResult<Void> unLikeBoard(@PathVariable("boardId") String boardId, @CurrentUserId Long userId) {
        commandService.unLike(boardId, userId);
        return ApiUtils.success();
    }

    @PreAuthorize("hasRole('USER')")
    @GetMapping("/users/me/liked-boards")
    public ApiResult<LikedBoardResponse> likedBoards(@RequestParam(value = "page", defaultValue = "0") int page,
                                                @CurrentUserId Long userId) {
        if (page < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "page must be 0 or greater");
        }
        return ApiUtils.success(LikedBoardResponse.from(queryService.likedBoards(userId, page)));
    }
}
