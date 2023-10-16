package com.tagstory.api.domain.comment.dto.response;

import com.tagstory.core.domain.comment.service.dto.Comment;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class CreateReplyResponse {
    private Long commentId;
    private String nickname;
    private String content;
    private LocalDateTime createdDate;

    public static CreateReplyResponse from(Comment comment) {
        return CreateReplyResponse.builder()
                .commentId(comment.getCommentId())
                .nickname(comment.getUser().getNickname())
                .content(comment.getContent())
                .createdDate(comment.getCreatedAt())
                .build();
    }
}
