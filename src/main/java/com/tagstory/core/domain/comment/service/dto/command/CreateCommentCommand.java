package com.tagstory.core.domain.comment.service.dto.command;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CreateCommentCommand {
    private Long userId;
    private String boardId;
    private String content;
}
