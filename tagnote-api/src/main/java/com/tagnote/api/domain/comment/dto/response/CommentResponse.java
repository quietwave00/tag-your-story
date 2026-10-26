package com.tagnote.api.domain.comment.dto.response;

import com.tagnote.core.domain.comment.service.Comment;
import com.tagnote.core.domain.user.service.User;
import lombok.Builder;
import lombok.Getter;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Getter
@Builder
public class CommentResponse {
    @Schema(description = "댓글 ID")
    private Long commentId;
    @Schema(description = "답글의 부모 댓글 ID. 최상위 댓글이면 null", nullable = true)
    private Long parentId;
    @Schema(description = "현재 작성자 표시명. 탈퇴한 사용자는 '탈퇴한 사용자'")
    private String nickname;
    @Schema(description = "댓글 본문")
    private String content;
    @Schema(description = "생성 시각. 현재 작성·수정·단독 답글 조회 변환에서는 null이며 목록에 포함된 자식 답글에서는 값이 있을 수 있음", nullable = true)
    private LocalDateTime createdDate;

    public static CommentResponse from(Comment comment) {
        Long parentId = (Objects.nonNull(comment.getParent())) ? comment.getParent().getCommentId() : null;
        return builder()
                .commentId(comment.getCommentId())
                .parentId(parentId)
                .nickname(comment.getUser().getDisplayNickname())
                .content(comment.getContent())
                .createdDate(comment.getCreatedAt())
                .build();
    }
}
