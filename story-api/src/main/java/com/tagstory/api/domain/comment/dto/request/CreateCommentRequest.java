package com.tagstory.api.domain.comment.dto.request;

import com.tagstory.core.domain.comment.service.dto.command.CreateCommentCommand;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCommentRequest {
    @NotBlank(message = "boardId는 비어 있을 수 없습니다.")
    private String boardId;

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
