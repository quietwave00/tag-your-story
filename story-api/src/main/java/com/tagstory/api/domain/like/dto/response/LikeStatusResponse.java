package com.tagstory.api.domain.like.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LikeStatusResponse {
    private boolean isLiked;

    public static LikeStatusResponse from(boolean isLiked) {
        return builder()
                .isLiked(isLiked)
                .build();
    }
}
