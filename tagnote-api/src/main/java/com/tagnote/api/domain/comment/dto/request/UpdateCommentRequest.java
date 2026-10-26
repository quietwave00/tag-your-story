package com.tagnote.api.domain.comment.dto.request;

import com.tagnote.core.domain.comment.service.dto.command.UpdateCommentCommand;
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
public class UpdateCommentRequest {

    @Schema(description = "수정할 댓글 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "commentId는 비어 있을 수 없습니다.")
    private Long commentId;

    @Schema(description = "새 댓글 본문. 공백만 입력할 수 없음", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "댓글 content는 비어 있을 수 없습니다.")
    private String content;

    public UpdateCommentCommand toCommand() {
        return UpdateCommentCommand.builder()
                .commentId(commentId)
                .content(content)
                .build();
    }
}
