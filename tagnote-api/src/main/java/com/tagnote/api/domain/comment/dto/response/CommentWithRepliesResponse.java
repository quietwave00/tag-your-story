package com.tagnote.api.domain.comment.dto.response;

import com.tagnote.core.domain.comment.service.Comment;
import com.tagnote.core.domain.comment.service.dto.response.CommentWithReplies;
import lombok.Builder;
import lombok.Getter;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Getter
@Builder
public class CommentWithRepliesResponse {
    @Schema(description = "최상위 댓글")
    private CommentResponse comment;
    @Schema(description = "POST 상태 답글 목록")
    private List<CommentResponse> children;

    public static CommentWithRepliesResponse from(CommentWithReplies commentWithReplies) {
        return CommentWithRepliesResponse.builder()
                .comment(CommentResponse.from(commentWithReplies.getComment()))
                .children(commentWithReplies.getChildren().stream().map(CommentResponse::from).toList())
                .build();
    }
}
