package com.tagstory.api.domain.comment.dto.request;

import com.tagstory.core.domain.comment.service.dto.command.CreateReplyCommand;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateReplyRequest {

    @NotBlank(message = "boardId는 비어 있을 수 없습니다.")
    private String boardId;

    @NotNull(message = "parentId는 비어 있을 수 없습니다.")
    private Long parentId;

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
