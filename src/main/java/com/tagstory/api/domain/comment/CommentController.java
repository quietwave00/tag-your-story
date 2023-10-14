package com.tagstory.api.domain.comment;

import com.tagstory.api.annotations.CurrentUserId;
import com.tagstory.api.domain.comment.dto.request.CreateCommentRequest;
import com.tagstory.api.domain.comment.dto.request.CreateReplyRequest;
import com.tagstory.api.domain.comment.dto.request.UpdateCommentRequest;
import com.tagstory.api.domain.comment.dto.response.CommentResponse;
import com.tagstory.api.utils.ApiUtils;
import com.tagstory.api.utils.dto.ApiResult;
import com.tagstory.core.domain.comment.service.CommentFacade;
import com.tagstory.core.domain.comment.service.dto.Comment;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/comments")
public class CommentController {
    private final CommentFacade commentFacade;

    /*
     * 댓글을 작성한다.
     */
    @PreAuthorize("hasRole('ROLE_USER')")
    @PostMapping
    public ApiResult<CommentResponse> create(@RequestBody CreateCommentRequest request, @CurrentUserId Long userId) {
        Comment comment = commentFacade.create(request.toCommand(userId));
        return ApiUtils.success(CommentResponse.from(comment));
    }

    /*
     * 댓글을 수정한다.
     */
    @PreAuthorize("hasRole('ROLE_USER')")
    @PatchMapping
    public ApiResult<CommentResponse> update(@RequestBody UpdateCommentRequest request) {
        Comment comment = commentFacade.update(request.toCommand());
        return ApiUtils.success(CommentResponse.from(comment));
    }


    /*
     * 답글을 작성한다.
     */
    @PreAuthorize("hasRole('ROLE_USER')")
    @PostMapping("/replies")
    public ApiResult<CommentResponse> createReply(@RequestBody CreateReplyRequest request, @CurrentUserId Long userId) {
        Comment comment = commentFacade.createReply(request.toCommand(userId));
        return ApiUtils.success(CommentResponse.from(comment));
    }


}
