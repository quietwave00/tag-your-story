package com.tagnote.core.domain.like.dto.command;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LikeBoardCommand {
    private String boardId;
    private Long userId;
}
