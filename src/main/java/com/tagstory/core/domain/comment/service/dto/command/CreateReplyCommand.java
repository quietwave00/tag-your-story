package com.tagstory.core.domain.comment.service.dto.command;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CreateReplyCommand {
    private Long userId;
    private String boardId;
    private Long parentId;
    private String content;
}
