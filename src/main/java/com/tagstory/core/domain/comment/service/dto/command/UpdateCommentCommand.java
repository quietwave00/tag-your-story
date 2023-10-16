package com.tagstory.core.domain.comment.service.dto.command;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UpdateCommentCommand {
    private Long commentId;
    private String content;
}
