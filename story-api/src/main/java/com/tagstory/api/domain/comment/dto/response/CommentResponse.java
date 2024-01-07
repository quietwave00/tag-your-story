package com.tagstory.api.domain.comment.dto.response;

import com.tagstory.core.domain.comment.service.Comment;
import com.tagstory.core.domain.user.service.User;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Getter
@Builder
public class CommentResponse {
    private Long commentId;
    private Long parentId;
    private User user;
    private String content;
    private LocalDateTime createdDate;
    private List<Comment> children;

    public static CommentResponse from(CompletableFuture<Comment> asyncComment) throws ExecutionException, InterruptedException {
        Comment comment = asyncComment.get();

        Long parentId = (Objects.nonNull(comment.getParent())) ? comment.getParent().getCommentId() : null;
        return builder()
                .commentId(comment.getCommentId())
                .parentId(parentId)
                .user(comment.getUser())
                .content(comment.getContent())
                .createdDate(comment.getCreatedAt())
                .children(comment.getChildren())
                .build();
    }

    public static CommentResponse from(Comment comment) {
        Long parentId = (Objects.nonNull(comment.getParent())) ? comment.getParent().getCommentId() : null;
        return builder()
                .commentId(comment.getCommentId())
                .parentId(parentId)
                .user(comment.getUser())
                .content(comment.getContent())
                .createdDate(comment.getCreatedAt())
                .children(comment.getChildren())
                .build();
    }
}
