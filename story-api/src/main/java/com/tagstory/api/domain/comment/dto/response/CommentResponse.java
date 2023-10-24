package com.tagstory.api.domain.comment.dto.response;

import com.tagstory.core.domain.comment.service.dto.Comment;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class CommentResponse {
    private Long commentId;
    private String nickname;
    private String content;
    private LocalDateTime createdDate;
    private List<Comment> children;

    public static CommentResponse from(Comment comment) {
        return builder()
                .commentId(comment.getCommentId())
                .nickname(comment.getUser().getNickname())
                .content(comment.getContent())
                .createdDate(comment.getCreatedAt())
                .children(comment.getChildren())
                .build();
    }
}
