package com.tagstory.api.domain.like.dto.response;

import com.tagstory.core.domain.like.dto.response.LikeCount;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LikeCountResponse {
    private int likeCount;

    public static LikeCountResponse from(LikeCount getLikeCount) {
        return LikeCountResponse.builder()
                .likeCount(getLikeCount.getLikeCount())
                .build();
    }
}
