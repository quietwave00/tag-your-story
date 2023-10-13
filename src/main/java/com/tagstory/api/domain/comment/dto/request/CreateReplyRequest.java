package com.tagstory.api.domain.comment.dto.request;

import com.tagstory.core.domain.comment.service.dto.command.CreateReplyCommand;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateReplyRequest {
    private String boardId;
    private Long parentId;
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
