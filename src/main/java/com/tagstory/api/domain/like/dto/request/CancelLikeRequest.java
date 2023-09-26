package com.tagstory.api.domain.like.dto.request;

import com.tagstory.core.domain.like.dto.command.CancelLikeCommand;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CancelLikeRequest {
    private String boardId;

    public CancelLikeCommand toCommand(Long userId) {
        return CancelLikeCommand.builder()
                .boardId(boardId)
                .userId(userId)
                .build();
    }
}
