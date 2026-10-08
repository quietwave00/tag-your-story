package com.tagnote.domain.user.fixture;

import com.tagnote.core.domain.user.service.User;

public class UserFixture {
    public static User createUser(Long userId) {
        return User.builder()
                .userId(userId)
                .build();
    }
}
