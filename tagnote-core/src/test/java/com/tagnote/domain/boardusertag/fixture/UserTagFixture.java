package com.tagnote.domain.boardusertag.fixture;

import com.tagnote.core.domain.usertag.UserTagEntity;
import com.tagnote.core.domain.user.UserEntity;

public class UserTagFixture {
    public static UserTagEntity createUserTagEntity(Long userTagId, String name) {
        return UserTagEntity.builder()
                .userTagId(userTagId)
                .owner(UserEntity.builder().userId(1L).build())
                .name(name)
                .build();
    }
}
