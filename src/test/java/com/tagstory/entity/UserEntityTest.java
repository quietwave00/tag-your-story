package com.tagstory.entity;

import com.tagstory.core.domain.user.Role;
import com.tagstory.core.domain.user.UserEntity;
import com.tagstory.core.domain.user.UserStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class UserEntityTest {

    @Test
    void 회원가입() {
        //given
        String userKey = "userKey";
        String email = "test@email.com";

        //then
        UserEntity result = UserEntity.register(userKey, email);
        assertThat(result.getUserKey()).isEqualTo(userKey);
        assertThat(result.getEmail()).isEqualTo(email);
        assertThat(result.getRole()).isEqualTo(Role.ROLE_USER);
        assertThat(result.getUserStatus()).isEqualTo(UserStatus.ACTIVE);

    }
}
