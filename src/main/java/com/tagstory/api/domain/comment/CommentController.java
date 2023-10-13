package com.tagstory.api.domain.comment;

import com.tagstory.api.annotations.CurrentUserId;
import com.tagstory.api.domain.comment.dto.request.CreateCommentRequest;
import com.tagstory.api.domain.comment.dto.response.CreateCommentResponse;
import com.tagstory.api.utils.ApiUtils;
import com.tagstory.api.utils.dto.ApiResult;
import com.tagstory.core.domain.comment.service.CommentFacade;
import com.tagstory.core.domain.comment.service.dto.Comment;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/comments")
public class CommentController {
    private final CommentFacade commentFacade;

    @PreAuthorize("hasRole('ROLE_USER')")
    @PostMapping
    public ApiResult<CreateCommentResponse> create(@RequestBody CreateCommentRequest request, @CurrentUserId Long userId) {
        Comment comment = commentFacade.create(request.toCommand(userId));
        return ApiUtils.success(CreateCommentResponse.from(comment));
    }
}
