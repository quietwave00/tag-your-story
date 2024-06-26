package com.tagstory.domain.user.fixture;

import com.tagstory.core.domain.user.service.User;

public class UserFixture {
    public static User createUser(Long userId) {
        return User.builder()
                .userId(userId)
                .build();
    }
}
