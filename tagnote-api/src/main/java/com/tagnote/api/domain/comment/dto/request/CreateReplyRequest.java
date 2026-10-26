package com.tagnote.api.domain.comment.dto.request;

import com.tagnote.core.domain.comment.service.dto.command.CreateReplyCommand;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import io.swagger.v3.oas.annotations.media.Schema;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateReplyRequest {

    @Schema(description = "답글을 저장할 POST 상태 게시글 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "boardId는 비어 있을 수 없습니다.")
    private String boardId;

    @Schema(description = "부모 댓글 ID. 현재 깊이나 Board 일치는 검증하지 않음", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "parentId는 비어 있을 수 없습니다.")
    private Long parentId;

    @Schema(description = "답글 본문. 공백만 입력할 수 없음", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "댓글 content는 비어 있을 수 없습니다.")
    private String content;

    public CreateReplyCommand toCommand(Long userId) {
        return CreateReplyCommand.builder()
                .userId(userId)
                .boardId(boardId)
                .parentId(parentId)
                .content(content)
                .build();
    }
}
