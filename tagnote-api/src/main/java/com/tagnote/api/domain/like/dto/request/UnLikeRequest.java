package com.tagnote.api.domain.like.dto.request;

import com.tagnote.core.domain.like.dto.command.UnLikeCommand;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import io.swagger.v3.oas.annotations.media.Schema;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UnLikeRequest {

    @NotBlank(message = "boardId는 비어 있을 수 없습니다.")
    @Schema(description = "좋아요를 취소할 게시글 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private String boardId;

    public UnLikeCommand toCommand(Long userId) {
        return UnLikeCommand.builder()
                .boardId(boardId)
                .userId(userId)
                .build();
    }
}
