package com.tagstory.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class UserTest {

    @Test
    void 회원가입() {
        //given
        String userKey = "userKey";
        String email = "test@email.com";
        String refreshToken = "refreshToken";

        //then
        User result = User.register(userKey, email, refreshToken);
        assertThat(result.getUserKey()).isEqualTo(userKey);
        assertThat(result.getEmail()).isEqualTo(email);
        assertThat(result.getRole()).isEqualTo(Role.ROLE_USER);
        assertThat(result.getUserStatus()).isEqualTo(UserStatus.ACTIVE);

    }
}
