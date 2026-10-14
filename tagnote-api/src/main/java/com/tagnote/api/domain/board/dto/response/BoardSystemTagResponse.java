package com.tagnote.api.domain.board.dto.response;

import com.tagnote.application.board.model.BoardSystemTag;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Board 대상 음악에 계산된 visible System Tag")
public record BoardSystemTagResponse(
        @Schema(description = "System Tag ID", example = "1") long tagId,
        @Schema(description = "표시 이름", example = "Ambient") String name,
        @Schema(description = "Resolver 점수", example = "0.9") double score
) {
    public static BoardSystemTagResponse from(BoardSystemTag tag) {
        return new BoardSystemTagResponse(tag.tagId(), tag.name(), tag.score());
    }
}
