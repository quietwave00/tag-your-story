package com.tagnote.application.auth;

import com.tagnote.application.auth.port.LoginAttemptStore;
import com.tagnote.core.domain.user.AccountProvider;
import com.tagnote.core.domain.user.Role;
import com.tagnote.core.domain.user.UserEntity;
import com.tagnote.core.domain.user.UserStatus;
import com.tagnote.core.domain.user.repository.UserRepository;
import com.tagnote.core.exception.CustomException;
import com.tagnote.core.exception.ExceptionCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class EmailLoginServiceTest {
    private final UserRepository users = mock(UserRepository.class);
    private final LoginAttemptStore attempts = mock(LoginAttemptStore.class);
    private final EmailLoginService service = new EmailLoginService(users, attempts);
    private final String hash = new BCryptPasswordEncoder().encode("Password123");

    @BeforeEach
    void allow() {
        when(attempts.allow(anyString(), anyString())).thenReturn(true);
    }

    @Test
    void normalizedEmailAndCorrectPasswordReturnActiveLocalUser() {
        when(users.findByEmail("user@example.com")).thenReturn(Optional.of(local(UserStatus.ACTIVE, hash)));
        assertThat(service.login(" User@Example.com ", "Password123", "192.0.2.1").getUserId()).isEqualTo(42L);
        verify(users).findByEmail("user@example.com");
        verify(attempts).clearFailures(anyString());
    }

    @Test
    void unknownEmailAndWrongPasswordUseSameErrorAndCountFailures() {
        when(users.findByEmail("user@example.com")).thenReturn(Optional.of(local(UserStatus.ACTIVE, hash)));
        assertError(() -> service.login("missing@example.com", "Password123", "192.0.2.1"),
                ExceptionCode.LOGIN_INVALID_CREDENTIALS);
        assertError(() -> service.login("user@example.com", "wrong", "192.0.2.1"),
                ExceptionCode.LOGIN_INVALID_CREDENTIALS);
        verify(attempts, times(2)).recordFailure(anyString());
        verify(attempts, never()).clearFailures(anyString());
    }

    @Test
    void googleAccountProvidesProviderGuidanceWithoutToken() {
        when(users.findByEmail("user@example.com")).thenReturn(Optional.of(UserEntity.builder()
                .provider(AccountProvider.GOOGLE).build()));
        assertError(() -> service.login("user@example.com", "Password123", "192.0.2.1"),
                ExceptionCode.GOOGLE_ACCOUNT_EXISTS);
        verify(attempts, never()).clearFailures(anyString());
    }

    @Test
    void dormantAccountCannotLogIn() {
        when(users.findByEmail("user@example.com")).thenReturn(Optional.of(local(UserStatus.DORMANT, hash)));
        assertError(() -> service.login("user@example.com", "Password123", "192.0.2.1"),
                ExceptionCode.USER_INACTIVE);
        verify(attempts, never()).clearFailures(anyString());
    }

    @Test
    void missingPasswordHashIsRejected() {
        when(users.findByEmail("user@example.com")).thenReturn(Optional.of(local(UserStatus.ACTIVE, null)));
        assertError(() -> service.login("user@example.com", "Password123", "192.0.2.1"),
                ExceptionCode.LOGIN_INVALID_CREDENTIALS);
    }

    @Test
    void rateLimitBlocksAccountLookup() {
        when(attempts.allow(anyString(), anyString())).thenReturn(false);
        assertError(() -> service.login("user@example.com", "Password123", "192.0.2.1"),
                ExceptionCode.LOGIN_RATE_LIMITED);
        verifyNoInteractions(users);
    }

    private UserEntity local(UserStatus status, String passwordHash) {
        return UserEntity.builder().userId(42L).email("user@example.com")
                .nickname("푸른여우4821").provider(AccountProvider.LOCAL).passwordHash(passwordHash)
                .role(Role.ROLE_USER).userStatus(status).build();
    }

    private void assertError(Runnable action, ExceptionCode code) {
        assertThatThrownBy(action::run).isInstanceOfSatisfying(CustomException.class,
                e -> assertThat(e.getExceptionCode()).isEqualTo(code));
    }
}
