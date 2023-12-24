package com.tagstory.api.domain.comment.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CommentCountResponse {
    int count;

    public static CommentCountResponse from(int count) {
        return builder()
                .count(count)
                .build();
    }
}

