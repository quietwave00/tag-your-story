package com.tagstory.api.domain.comment.dto.request;

import com.tagstory.core.domain.comment.service.dto.command.CreateCommentCommand;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCommentRequest {
    private String boardId;
    private String content;

    public CreateCommentCommand toCommand(Long userId) {
        return CreateCommentCommand.builder()
                .userId(userId)
                .boardId(boardId)
                .content(content)
                .build();
    }
}
