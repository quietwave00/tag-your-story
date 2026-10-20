package com.tagnote.application.auth;

import com.tagnote.application.auth.port.SignupChallengeStore;
import com.tagnote.core.domain.user.AccountProvider;
import com.tagnote.core.domain.user.Role;
import com.tagnote.core.domain.user.UserEntity;
import com.tagnote.core.domain.user.UserStatus;
import com.tagnote.core.domain.user.repository.UserRepository;
import com.tagnote.core.domain.user.service.GoogleNicknameGenerator;
import com.tagnote.core.domain.user.service.User;
import com.tagnote.core.exception.CustomException;
import com.tagnote.core.exception.ExceptionCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class EmailSignupServiceTest {
    private final SignupChallengeService challenges = mock(SignupChallengeService.class);
    private final SignupChallengeStore store = mock(SignupChallengeStore.class);
    private final UserRepository users = mock(UserRepository.class);
    private final LocalAccountWriter writer = mock(LocalAccountWriter.class);
    private final GoogleNicknameGenerator nicknames = mock(GoogleNicknameGenerator.class);
    private final SignupCodeVerifier signer = new SignupCodeVerifier("0123456789abcdef0123456789abcdef");
    private EmailSignupService service;
    private final String id = "53e65cc8-8e8e-49eb-a309-c084566a8051";

    @BeforeEach
    void setUp() {
        service = new EmailSignupService(challenges, store, signer, users, writer, nicknames);
        when(store.allowAvailability(anyString(), anyString())).thenReturn(true);
        when(nicknames.generate()).thenReturn("푸른여우4821");
    }

    @Test
    void startNormalizesEmailAndStoresOnlyEncodedPassword() {
        when(challenges.start(anyString(), anyString(), anyString())).thenReturn(id);
        assertThat(service.start(" User@Example.com ", "Password123", "Password123", "192.0.2.1")).isEqualTo(id);
        verify(users).findByEmail("user@example.com");
        verify(challenges).start(eq("user@example.com"), argThat(hash -> hash.startsWith("$2")
                && !hash.contains("Password123") && new BCryptPasswordEncoder().matches("Password123", hash)),
                eq("192.0.2.1"));
    }

    @Test
    void invalidPasswordNeverIssuesChallenge() {
        assertThatThrownBy(() -> service.start("user@example.com", "password", "different", "192.0.2.1"))
                .isInstanceOfSatisfying(CustomException.class,
                        e -> assertThat(e.getExceptionCode()).isEqualTo(ExceptionCode.SIGNUP_INPUT_INVALID));
        verifyNoInteractions(challenges);
    }

    @Test
    void googleAccountReturnsProviderErrorForStartAndAvailability() {
        when(users.findByEmail("user@example.com")).thenReturn(Optional.of(UserEntity.builder()
                .provider(AccountProvider.GOOGLE).build()));
        assertThatThrownBy(() -> service.start(" USER@example.com ", "Password123", "Password123", "192.0.2.1"))
                .isInstanceOfSatisfying(CustomException.class,
                        e -> assertThat(e.getExceptionCode()).isEqualTo(ExceptionCode.GOOGLE_ACCOUNT_EXISTS));
        assertThatThrownBy(() -> service.available(" USER@example.com ", "192.0.2.1"))
                .isInstanceOfSatisfying(CustomException.class,
                        e -> assertThat(e.getExceptionCode()).isEqualTo(ExceptionCode.GOOGLE_ACCOUNT_EXISTS));
        verifyNoInteractions(challenges);
    }

    @Test
    void availabilityLimitBlocksEnumerationBeforeLookup() {
        when(store.allowAvailability(anyString(), anyString())).thenReturn(false);
        assertThatThrownBy(() -> service.available("user@example.com", "192.0.2.1"))
                .isInstanceOfSatisfying(CustomException.class,
                        e -> assertThat(e.getExceptionCode()).isEqualTo(ExceptionCode.SIGNUP_RATE_LIMITED));
        verifyNoInteractions(users);
    }

    @Test
    void validCodeCreatesLocalUserAndRepeatedCodeReturnsSameAccount() {
        String verifier = signer.sign("code", id + ":123456");
        String key = "local:" + signer.sign("local-user-key", id);
        User account = User.builder().userId(42L).userKey(key)
                .provider(AccountProvider.LOCAL).email("user@example.com")
                .nickname("푸른여우4821").role(Role.ROLE_USER).userStatus(UserStatus.ACTIVE).build();
        when(store.claim(eq(id), eq(verifier), anyString())).thenReturn(new SignupChallengeStore.Claim(
                SignupChallengeStore.Claim.Status.CLAIMED, "user@example.com", "$2a$hash", null),
                new SignupChallengeStore.Claim(SignupChallengeStore.Claim.Status.COMPLETE, null, null, 42L));
        when(writer.create(key, "user@example.com", "$2a$hash", "푸른여우4821")).thenReturn(account);
        when(users.findById(42L)).thenReturn(Optional.of(UserEntity.builder().userId(42L)
                .userKey(key).provider(AccountProvider.LOCAL).email("user@example.com")
                .nickname("푸른여우4821").role(Role.ROLE_USER).userStatus(UserStatus.ACTIVE).build()));
        assertThat(service.confirm(id, "123456").getUserId()).isEqualTo(42L);
        assertThat(service.confirm(id, "123456").getUserId()).isEqualTo(42L);
        verify(writer, times(1)).create(key, "user@example.com", "$2a$hash", "푸른여우4821");
        verify(store).complete(eq(id), eq(verifier), anyString(), eq(42L));
    }

    @Test
    void dbFailureReleasesClaimForRetry() {
        String verifier = signer.sign("code", id + ":123456");
        when(store.claim(eq(id), eq(verifier), anyString())).thenReturn(new SignupChallengeStore.Claim(
                SignupChallengeStore.Claim.Status.CLAIMED, "user@example.com", "hash", null));
        when(writer.create(anyString(), anyString(), anyString(), anyString()))
                .thenThrow(new DataIntegrityViolationException("constraint"));
        assertThatThrownBy(() -> service.confirm(id, "123456")).isInstanceOf(CustomException.class);
        verify(store).release(eq(id), eq(verifier), anyString());
    }

    @Test
    void committedAccountIsRecoveredAfterRedisCompletionFailure() {
        String verifier = signer.sign("code", id + ":123456");
        String key = "local:" + signer.sign("local-user-key", id);
        UserEntity saved = UserEntity.builder().userId(42L).userKey(key)
                .provider(AccountProvider.LOCAL).email("user@example.com")
                .nickname("푸른여우4821").role(Role.ROLE_USER).userStatus(UserStatus.ACTIVE).build();
        when(store.claim(eq(id), eq(verifier), anyString())).thenReturn(new SignupChallengeStore.Claim(
                SignupChallengeStore.Claim.Status.CLAIMED, "user@example.com", "hash", null));
        when(users.findByUserKey(key)).thenReturn(Optional.of(saved));
        assertThat(service.confirm(id, "123456").getUserId()).isEqualTo(42L);
        verifyNoInteractions(writer);
        verify(store).complete(eq(id), eq(verifier), anyString(), eq(42L));
    }

    @Test
    void googleSignupBetweenStartAndConfirmDoesNotCreateLocalAccount() {
        String verifier = signer.sign("code", id + ":123456");
        when(store.claim(eq(id), eq(verifier), anyString())).thenReturn(new SignupChallengeStore.Claim(
                SignupChallengeStore.Claim.Status.CLAIMED, "user@example.com", "hash", null));
        when(users.findByEmail("user@example.com")).thenReturn(Optional.of(UserEntity.builder()
                .provider(AccountProvider.GOOGLE).build()));
        assertThatThrownBy(() -> service.confirm(id, "123456"))
                .isInstanceOfSatisfying(CustomException.class,
                        e -> assertThat(e.getExceptionCode()).isEqualTo(ExceptionCode.GOOGLE_ACCOUNT_EXISTS));
        verifyNoInteractions(writer);
        verify(store).release(eq(id), eq(verifier), anyString());
    }

    @Test
    void nicknameUniqueConflictRetriesInNewWriteAttempt() {
        String verifier = signer.sign("code", id + ":123456");
        String key = "local:" + signer.sign("local-user-key", id);
        when(store.claim(eq(id), eq(verifier), anyString())).thenReturn(new SignupChallengeStore.Claim(
                SignupChallengeStore.Claim.Status.CLAIMED, "user@example.com", "hash", null));
        when(nicknames.generate()).thenReturn("푸른여우4821", "밝은수달5832");
        when(users.existsByNickname("푸른여우4821")).thenReturn(true);
        when(writer.create(key, "user@example.com", "hash", "푸른여우4821"))
                .thenThrow(new DataIntegrityViolationException("nickname"));
        when(writer.create(key, "user@example.com", "hash", "밝은수달5832"))
                .thenReturn(User.builder().userId(42L).nickname("밝은수달5832").build());

        assertThat(service.confirm(id, "123456").getNickname()).isEqualTo("밝은수달5832");
        verify(writer, times(2)).create(eq(key), eq("user@example.com"), eq("hash"), anyString());
    }
}
