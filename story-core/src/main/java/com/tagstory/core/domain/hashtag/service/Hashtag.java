package com.tagstory.core.domain.hashtag.service;

import com.tagstory.core.domain.hashtag.HashtagEntity;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class Hashtag {
    private Long hashtagId;

    private String name;

    /*
     * 형변환
     */
    public HashtagEntity toEntity() {
        return HashtagEntity.builder()
                .hashtagId(this.getHashtagId())
                .name(this.getName())
                .build();
    }
}
