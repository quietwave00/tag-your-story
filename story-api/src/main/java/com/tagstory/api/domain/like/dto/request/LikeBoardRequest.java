package com.tagstory.api.domain.like.dto.request;

import com.tagstory.core.domain.like.dto.command.LikeBoardCommand;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LikeBoardRequest {

    @NotBlank(message = "boardId는 비어 있을 수 없습니다.")
    private String boardId;

    public LikeBoardCommand toCommand(Long userId) {
        return LikeBoardCommand.builder()
                .boardId(boardId)
                .userId(userId)
                .build();
    }
}
