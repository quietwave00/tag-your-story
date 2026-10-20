package com.tagnote.domain.user.service;

import com.tagnote.core.domain.user.AccountProvider;
import com.tagnote.core.domain.user.Role;
import com.tagnote.core.domain.user.UserEntity;
import com.tagnote.core.domain.user.UserStatus;
import com.tagnote.core.domain.user.repository.UserRepository;
import com.tagnote.core.domain.user.service.GoogleAccountService;
import com.tagnote.core.domain.user.service.GoogleAccountWriter;
import com.tagnote.core.domain.user.service.GoogleNicknameGenerator;
import com.tagnote.core.domain.user.service.User;
import com.tagnote.core.exception.CustomException;
import com.tagnote.core.exception.ExceptionCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GoogleAccountServiceTest {
    @Mock UserRepository users;
    @Mock GoogleAccountWriter writer;
    @Mock GoogleNicknameGenerator nicknames;

    private GoogleAccountService service() { return new GoogleAccountService(users, writer, nicknames); }

    @Test
    void new_google_account_is_active_user_with_normalized_email() {
        when(nicknames.generate()).thenReturn("푸른여우4821");
        User saved = User.builder().userId(7L).nickname("푸른여우4821")
                .role(Role.ROLE_USER).userStatus(UserStatus.ACTIVE).build();
        when(writer.create("sub-1", "user@example.com", "푸른여우4821")).thenReturn(saved);

        assertThat(service().loginOrCreate("sub-1", " User@Example.com ")).isSameAs(saved);
    }

    @Test
    void returning_subject_preserves_id_nickname_and_stored_email() {
        UserEntity existing = google(7L, "sub-1", "old@example.com", "푸른여우4821");
        when(users.findByProviderAndSubject(AccountProvider.GOOGLE, "sub-1")).thenReturn(Optional.of(existing));

        User result = service().loginOrCreate("sub-1", "new@example.com");
        assertThat(result.getUserId()).isEqualTo(7L);
        assertThat(result.getNickname()).isEqualTo("푸른여우4821");
        assertThat(result.getEmail()).isEqualTo("old@example.com");
        verifyNoInteractions(writer);
    }

    @Test
    void local_email_conflict_never_creates_account() {
        when(users.findByEmail("user@example.com")).thenReturn(Optional.of(UserEntity.builder()
                .provider(AccountProvider.LOCAL).build()));
        assertThatThrownBy(() -> service().loginOrCreate("sub-2", " USER@example.com "))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException)e).getExceptionCode())
                        .isEqualTo(ExceptionCode.GOOGLE_EMAIL_CONFLICT_LOCAL));
        verifyNoInteractions(writer);
    }

    @Test
    void different_google_subject_email_conflict_never_creates_account() {
        when(users.findByEmail("user@example.com")).thenReturn(Optional.of(google(7L, "sub-1", "user@example.com", "one")));
        assertThatThrownBy(() -> service().loginOrCreate("sub-2", "user@example.com"))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException)e).getExceptionCode())
                        .isEqualTo(ExceptionCode.GOOGLE_EMAIL_CONFLICT_GOOGLE));
        verifyNoInteractions(writer);
    }

    @Test
    void nickname_unique_conflict_retries_with_new_candidate() {
        when(nicknames.generate()).thenReturn("푸른여우4821", "밝은수달4912");
        when(writer.create("sub-1", "user@example.com", "푸른여우4821"))
                .thenThrow(new DataIntegrityViolationException("nickname unique"));
        when(users.existsByNickname("푸른여우4821")).thenReturn(true);
        when(writer.create("sub-1", "user@example.com", "밝은수달4912"))
                .thenReturn(User.builder().userId(7L).nickname("밝은수달4912").build());
        assertThat(service().loginOrCreate("sub-1", "user@example.com").getNickname())
                .isEqualTo("밝은수달4912");
        verify(writer, times(2)).create(eq("sub-1"), eq("user@example.com"), anyString());
    }

    @Test
    void concurrent_same_subject_returns_committed_account_after_unique_conflict() {
        UserEntity committed = google(7L, "sub-1", "user@example.com", "푸른여우4821");
        when(nicknames.generate()).thenReturn("밝은수달4912");
        when(writer.create("sub-1", "user@example.com", "밝은수달4912"))
                .thenThrow(new DataIntegrityViolationException("subject unique"));
        when(users.findByProviderAndSubject(AccountProvider.GOOGLE, "sub-1"))
                .thenReturn(Optional.empty(), Optional.of(committed));
        assertThat(service().loginOrCreate("sub-1", "user@example.com").getUserId()).isEqualTo(7L);
    }

    @Test
    void nickname_collisions_stop_after_five_separate_attempts() {
        when(nicknames.generate()).thenReturn("푸른여우4821");
        when(writer.create("sub-1", "user@example.com", "푸른여우4821"))
                .thenThrow(new DataIntegrityViolationException("nickname unique"));
        when(users.existsByNickname("푸른여우4821")).thenReturn(true);

        assertThatThrownBy(() -> service().loginOrCreate("sub-1", "user@example.com"))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException)e).getExceptionCode())
                        .isEqualTo(ExceptionCode.GOOGLE_SIGNUP_CONFLICT));
        verify(writer, times(5)).create("sub-1", "user@example.com", "푸른여우4821");
    }

    @Test
    void inactive_google_account_cannot_log_in() {
        when(users.findByProviderAndSubject(AccountProvider.GOOGLE, "sub-1"))
                .thenReturn(Optional.of(UserEntity.builder().role(Role.ROLE_USER)
                        .userStatus(UserStatus.DORMANT).build()));
        assertThatThrownBy(() -> service().loginOrCreate("sub-1", "user@example.com"))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException)e).getExceptionCode())
                        .isEqualTo(ExceptionCode.USER_INACTIVE));
    }

    private UserEntity google(Long id, String sub, String email, String nickname) {
        return UserEntity.builder().userId(id).userKey(sub).provider(AccountProvider.GOOGLE)
                .subject(sub).email(email).nickname(nickname).role(Role.ROLE_USER)
                .userStatus(UserStatus.ACTIVE).build();
    }
}
