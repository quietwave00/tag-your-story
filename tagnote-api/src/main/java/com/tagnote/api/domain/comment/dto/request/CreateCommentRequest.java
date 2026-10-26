package com.tagnote.api.domain.comment.dto.request;

import com.tagnote.core.domain.comment.service.dto.command.CreateCommentCommand;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import io.swagger.v3.oas.annotations.media.Schema;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCommentRequest {
    @Schema(description = "댓글을 달 POST 상태 게시글의 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "boardId는 비어 있을 수 없습니다.")
    private String boardId;

    @Schema(description = "댓글 본문. 공백만 입력할 수 없음", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "댓글 content는 비어 있을 수 없습니다.")
    private String content;

    public CreateCommentCommand toCommand(Long userId) {
        return CreateCommentCommand.builder()
                .userId(userId)
                .boardId(boardId)
                .content(content)
                .build();
    }
}
