package com.tagnote.domain.boardusertag.fixture;

import com.tagnote.core.domain.usertag.UserTagEntity;

public class UserTagFixture {
    public static UserTagEntity createUserTagEntity(Long userTagId, String name) {
        return UserTagEntity.builder()
                .userTagId(userTagId)
                .name(name)
                .build();
    }
}
