package com.tagstory.api.domain.like.dto.request;

import com.tagstory.core.domain.like.dto.command.LikeBoardCommand;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LikeBoardRequest {
    private Long boardId;

    public LikeBoardCommand toCommand(Long userId) {
        return LikeBoardCommand.builder()
                .boardId(boardId)
                .userId(userId)
                .build();
    }
}
