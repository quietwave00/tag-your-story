package com.tagnote.application.user;

import com.tagnote.core.common.CacheSpec;
import com.tagnote.core.common.CommonRedisTemplate;
import com.tagnote.core.domain.user.AccountProvider;
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
class AccountWithdrawalTest {
    @Mock UserRepository users;
    @Mock CommonRedisTemplate redis;
    @Mock AccountWithdrawalWriter writer;

    @Test
    void google_identity_is_released_after_locked_write() {
        UserEntity user = UserEntity.builder().userId(7L).userKey("google-sub")
                .subject("google-sub").provider(AccountProvider.GOOGLE)
                .email("user@example.com").passwordHash("hash").nickname("old")
                .role(Role.ROLE_USER).userStatus(UserStatus.ACTIVE).build();
        when(users.findByUserIdForUpdate(7L)).thenReturn(Optional.of(user));

        assertThat(new AccountWithdrawalWriter(users).withdraw(7L)).isEqualTo("google-sub");
        assertThat(user.getUserStatus()).isEqualTo(UserStatus.WITHDRAWN);
        assertThat(user.getUserKey()).isEqualTo("withdrawn:7");
        assertThat(user.getSubject()).isEqualTo("withdrawn:7");
        assertThat(user.getEmail()).isEqualTo("withdrawn+7@invalid.local");
        assertThat(user.getNickname()).isNull();
        assertThat(user.getPasswordHash()).isNull();
        assertThat(user.getDisplayNickname()).isEqualTo("탈퇴한 사용자");
        verify(users).flush();
    }

    @Test
    void local_subject_stays_null_and_repeated_withdrawal_is_rejected() {
        UserEntity user = UserEntity.builder().userId(8L).userKey("local-key")
                .provider(AccountProvider.LOCAL).email("local@example.com")
                .role(Role.ROLE_USER).userStatus(UserStatus.ACTIVE).build();
        when(users.findByUserIdForUpdate(8L)).thenReturn(Optional.of(user));
        AccountWithdrawalWriter withdrawal = new AccountWithdrawalWriter(users);

        withdrawal.withdraw(8L);
        assertThat(user.getSubject()).isNull();
        assertThatThrownBy(() -> withdrawal.withdraw(8L)).isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getExceptionCode())
                        .isEqualTo(ExceptionCode.ACCOUNT_WITHDRAWN));
        verify(users).flush();
    }

    @Test
    void redis_cleanup_failure_does_not_undo_committed_withdrawal() {
        when(writer.withdraw(7L)).thenReturn("old-key");
        doThrow(new IllegalStateException("redis down")).when(redis).delete(7L, CacheSpec.REFRESH_TOKEN);

        new AccountWithdrawalService(writer, redis).withdraw(7L);

        verify(redis).delete(7L, CacheSpec.USER);
        verify(redis).delete("old-key", CacheSpec.USER);
    }

    @Test
    void database_failure_does_not_touch_redis() {
        when(writer.withdraw(7L)).thenThrow(new IllegalStateException("db down"));
        assertThatThrownBy(() -> new AccountWithdrawalService(writer, redis).withdraw(7L))
                .isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(redis);
    }
}
