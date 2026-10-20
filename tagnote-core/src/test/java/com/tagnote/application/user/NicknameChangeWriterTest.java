package com.tagnote.application.user;

import com.tagnote.core.domain.user.Role;
import com.tagnote.core.domain.user.UserEntity;
import com.tagnote.core.domain.user.UserStatus;
import com.tagnote.core.domain.user.repository.UserRepository;
import com.tagnote.core.exception.CustomException;
import com.tagnote.core.exception.ExceptionCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NicknameChangeWriterTest {
    @Mock UserRepository users;

    @Test
    void changes_only_locked_active_user_and_flushes_unique_constraint() {
        UserEntity user = active("before");
        when(users.findByUserIdForUpdate(7L)).thenReturn(Optional.of(user));

        assertThat(new NicknameChangeWriter(users).change(7L, "after").getNickname()).isEqualTo("after");
        verify(users).flush();
        verify(users).findByUserIdForUpdate(7L);
    }

    @Test
    void current_nickname_is_no_op_success() {
        UserEntity user = active("same");
        when(users.findByUserIdForUpdate(7L)).thenReturn(Optional.of(user));

        assertThat(new NicknameChangeWriter(users).change(7L, "same").getNickname()).isEqualTo("same");
        verify(users, never()).flush();
    }

    @Test
    void inactive_user_cannot_change_nickname() {
        UserEntity user = UserEntity.builder().userStatus(UserStatus.DORMANT)
                .role(Role.ROLE_USER).nickname("before").build();
        when(users.findByUserIdForUpdate(7L)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> new NicknameChangeWriter(users).change(7L, "after"))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException)e).getExceptionCode())
                        .isEqualTo(ExceptionCode.USER_INACTIVE));
        assertThat(user.getNickname()).isEqualTo("before");
        verify(users, never()).flush();
    }

    private UserEntity active(String nickname) {
        return UserEntity.builder().userId(7L).nickname(nickname)
                .role(Role.ROLE_USER).userStatus(UserStatus.ACTIVE).build();
    }
}
