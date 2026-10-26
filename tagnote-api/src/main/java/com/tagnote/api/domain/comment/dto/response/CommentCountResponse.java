package com.tagnote.api.domain.comment.dto.response;

import lombok.Builder;
import lombok.Getter;
import io.swagger.v3.oas.annotations.media.Schema;

@Getter
@Builder
public class CommentCountResponse {
    @Schema(description = "POST 상태 최상위 댓글과 답글의 합계")
    int count;

    public static CommentCountResponse from(int count) {
        return builder()
                .count(count)
                .build();
    }
}

