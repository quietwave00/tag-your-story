package com.tagstory.api.domain.like.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LikeCountResponse {
    private int likeCount;

    public static LikeCountResponse from(int likeCount) {
        return builder()
                .likeCount(likeCount)
                .build();
    }
}
