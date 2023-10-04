package com.tagstory.api.domain.like;

import com.tagstory.api.annotations.CurrentUserId;
import com.tagstory.api.domain.like.dto.request.CancelLikeRequest;
import com.tagstory.api.domain.like.dto.request.LikeBoardRequest;
import com.tagstory.api.domain.like.dto.response.LikeCountResponse;
import com.tagstory.api.domain.like.dto.response.LikeStatusResponse;
import com.tagstory.api.utils.ApiUtils;
import com.tagstory.api.utils.dto.ApiResult;
import com.tagstory.core.domain.like.service.LikeFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class LikeController {
    private final LikeFacade likeFacade;

    /*
     * 게시글에 좋아요를 등록한다.
     */
    @PreAuthorize("hasRole('ROLE_USER')")
    @PostMapping("/likes")
    public ApiResult<Void> like(@RequestBody LikeBoardRequest likeBoardRequest, @CurrentUserId Long userId) {
        likeFacade.like(likeBoardRequest.toCommand(userId));
        return ApiUtils.success();
    }

    /*
     * 게시글에 좋아요를 취소한다.
     */
    @PreAuthorize("hasRole('ROLE_USER')")
    @DeleteMapping("/likes")
    public ApiResult<Void> cancelLike(@RequestBody CancelLikeRequest cancelLikeRequest, @CurrentUserId Long userId) {
        likeFacade.cancelLike(cancelLikeRequest.toCommand(userId));
        return ApiUtils.success();
    }

    /*
     * 게시글의 좋아요 개수를 가져온다.
     */
    @GetMapping("/likes/{boardId}")
    public ApiResult<LikeCountResponse> like(@PathVariable("boardId") String boardId) {
        int likeCount = likeFacade.getLikeCount(boardId);
        return ApiUtils.success(LikeCountResponse.from(likeCount));
    }

    /*
     * 사용자의 좋아요 여부를 체크한다.
     */
    @PreAuthorize("hasRole('ROLE_USER')")
    @GetMapping("/likes/status/{boardId}")
    public ApiResult<LikeStatusResponse> isLiked(@PathVariable("boardId") String boardId, @CurrentUserId Long userId) {
        boolean isLiked = likeFacade.isLiked(boardId, userId);
        return ApiUtils.success(LikeStatusResponse.from(isLiked));
    }
}
