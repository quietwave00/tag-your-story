package com.tagstory.core.domain.like.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LikeCount {
    private int likeCount;

    public static LikeCount onComplete(int likeCount) {
        return builder()
                .likeCount(likeCount)
                .build();
    }
}
