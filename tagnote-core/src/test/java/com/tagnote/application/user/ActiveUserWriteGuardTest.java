package com.tagnote.application.user;

import com.tagnote.core.domain.user.Role;
import com.tagnote.core.domain.user.UserEntity;
import com.tagnote.core.domain.user.UserStatus;
import com.tagnote.core.domain.user.repository.UserRepository;
import com.tagnote.core.exception.CustomException;
import com.tagnote.core.exception.ExceptionCode;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class ActiveUserWriteGuardTest {
    private final UserRepository users = mock(UserRepository.class);
    private final ActiveUserWriteGuard guard = new ActiveUserWriteGuard(users);

    @Test
    void locks_active_user_for_content_write() {
        UserEntity active = UserEntity.builder().userId(1L).role(Role.ROLE_USER)
                .userStatus(UserStatus.ACTIVE).build();
        when(users.findByUserIdForUpdate(1L)).thenReturn(Optional.of(active));
        assertThat(guard.lock(1L)).isSameAs(active);
    }

    @Test
    void refuses_withdrawn_user_even_after_request_authentication() {
        when(users.findByUserIdForUpdate(1L)).thenReturn(Optional.of(UserEntity.builder()
                .userId(1L).role(Role.ROLE_USER).userStatus(UserStatus.WITHDRAWN).build()));
        assertThatThrownBy(() -> guard.lock(1L)).isInstanceOf(CustomException.class)
                .satisfies(error -> assertThat(((CustomException) error).getExceptionCode())
                        .isEqualTo(ExceptionCode.ACCOUNT_WITHDRAWN));
    }
}
