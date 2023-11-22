package com.tagstory.core.domain.like.dto.command;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UnLikeCommand {
    private String boardId;
    private Long userId;
}
