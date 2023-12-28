package com.tagstory.api.domain.comment.dto.request;

import com.tagstory.core.domain.comment.service.dto.command.UpdateCommentCommand;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCommentRequest {

    @NotNull(message = "commentId는 비어 있을 수 없습니다.")
    private Long commentId;

    @NotBlank(message = "댓글 content는 비어 있을 수 없습니다.")
    private String content;

    public UpdateCommentCommand toCommand() {
        return UpdateCommentCommand.builder()
                .commentId(commentId)
                .content(content)
                .build();
    }
}
