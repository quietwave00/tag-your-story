package com.tagstory.api.domain.comment.dto.response;

import com.tagstory.core.domain.comment.service.dto.Comment;
import com.tagstory.core.domain.user.service.dto.response.User;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Getter
@Builder
public class CommentResponse {
    private Long commentId;
    private Long parentId;
    private User user;
    private String content;
    private LocalDateTime createdDate;
    private List<Comment> children;

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
