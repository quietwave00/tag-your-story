package com.tagstory.domain.boardhashtag.fixture;

import com.tagstory.core.domain.hashtag.HashtagEntity;

public class HashtagFixture {
    public static HashtagEntity createHashtagEntity(Long hashtagId, String name) {
        return HashtagEntity.builder()
                .hashtagId(hashtagId)
                .name(name)
                .build();
    }
}
