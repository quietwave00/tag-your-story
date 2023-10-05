package com.tagstory.api.domain.board.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BoardCountResponse {
    int count;

    public static BoardCountResponse from(int count) {
        return BoardCountResponse.builder()
                .count(count)
                .build();
    }
}
