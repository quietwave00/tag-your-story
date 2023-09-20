package com.tagstory.core.domain.like.dto.command;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CancelLikeCommand {
    private Long boardId;
    private Long userId;
}
