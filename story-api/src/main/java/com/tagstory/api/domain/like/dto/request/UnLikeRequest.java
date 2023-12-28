package com.tagstory.api.domain.like.dto.request;

import com.tagstory.core.domain.like.dto.command.UnLikeCommand;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UnLikeRequest {

    @NotBlank(message = "boardId는 비어 있을 수 없습니다.")
    private String boardId;

    public UnLikeCommand toCommand(Long userId) {
        return UnLikeCommand.builder()
                .boardId(boardId)
                .userId(userId)
                .build();
    }
}
