package com.tagstory.api.domain.like;

import com.tagstory.api.annotations.CurrentUserId;
import com.tagstory.api.domain.like.dto.request.UnLikeRequest;
import com.tagstory.api.domain.like.dto.request.LikeBoardRequest;
import com.tagstory.api.domain.like.dto.response.LikeStatusResponse;
import com.tagstory.core.domain.like.service.LikeFacade;
import com.tagstory.core.utils.api.ApiUtils;
import com.tagstory.core.utils.api.ApiResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class LikeController {
    private final LikeFacade likeFacade;

    /*
     * 게시글에 좋아요를 등록한다.
     */
    @PreAuthorize("hasRole('USER')")
    @PostMapping("/likes")
    public ApiResult<Boolean> like(@RequestBody @Valid LikeBoardRequest likeBoardRequest, @CurrentUserId Long userId) {
        Boolean result = likeFacade.like(likeBoardRequest.toCommand(userId));
        return ApiUtils.success(result);
    }

    /*
     * 게시글에 좋아요를 취소한다.
     */
    @PreAuthorize("hasRole('USER')")
    @DeleteMapping("/likes")
    public ApiResult<Void> unLike(@RequestBody @Valid UnLikeRequest request, @CurrentUserId Long userId) {
        likeFacade.unLike(request.toCommand(userId));
        return ApiUtils.success();
    }

    /*
     * 사용자의 좋아요 여부를 체크한다.
     */
    @PreAuthorize("hasRole('USER')")
    @GetMapping("/likes/status/{boardId}")
    public ApiResult<LikeStatusResponse> isLiked(@PathVariable("boardId") String boardId, @CurrentUserId Long userId) {
        boolean isLiked = likeFacade.isLiked(boardId, userId);
        return ApiUtils.success(LikeStatusResponse.from(isLiked));
    }
}
