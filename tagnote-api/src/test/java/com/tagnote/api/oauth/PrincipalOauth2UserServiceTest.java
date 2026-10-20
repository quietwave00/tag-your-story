package com.tagnote.api.oauth;

import com.tagnote.core.domain.user.Role;
import com.tagnote.core.domain.user.service.GoogleAccountService;
import com.tagnote.core.domain.user.service.User;
import com.tagnote.core.exception.CustomException;
import com.tagnote.core.exception.ExceptionCode;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class PrincipalOauth2UserServiceTest {
    private final GoogleAccountService accounts = mock(GoogleAccountService.class);
    private final PrincipalOauth2UserService service = new PrincipalOauth2UserService(accounts);

    @Test
    void verified_google_user_receives_user_principal() {
        when(accounts.loginOrCreate("sub-1", " User@Example.com "))
                .thenReturn(User.builder().userId(42L).role(Role.ROLE_USER).build());

        var principal = service.login(Map.of("sub", "sub-1", "email", " User@Example.com ", "email_verified", true));

        assertThat(principal.getUserId()).isEqualTo(42L);
        assertThat(principal.getAuthorities()).extracting("authority").containsExactly("ROLE_USER");
    }

    @Test
    void unverified_email_never_calls_account_service() {
        assertThatThrownBy(() -> service.login(Map.of("sub", "sub-1", "email", "x@y.com", "email_verified", false)))
                .isInstanceOf(OAuth2AuthenticationException.class)
                .satisfies(error -> assertThat(((OAuth2AuthenticationException) error).getError().getErrorCode())
                        .isEqualTo("GOOGLE_EMAIL_UNVERIFIED"));
        verifyNoInteractions(accounts);
    }

    @Test
    void missing_subject_or_email_never_creates_account() {
        assertThatThrownBy(() -> service.login(Map.of("email", "x@y.com", "email_verified", true)))
                .isInstanceOf(OAuth2AuthenticationException.class)
                .satisfies(error -> assertThat(((OAuth2AuthenticationException) error).getError().getErrorCode())
                        .isEqualTo("GOOGLE_IDENTITY_INVALID"));
        assertThatThrownBy(() -> service.login(Map.of("sub", "sub-1", "email_verified", true)))
                .isInstanceOf(OAuth2AuthenticationException.class)
                .satisfies(error -> assertThat(((OAuth2AuthenticationException) error).getError().getErrorCode())
                        .isEqualTo("GOOGLE_EMAIL_UNVERIFIED"));
        verifyNoInteractions(accounts);
    }

    @Test
    void provider_conflict_is_returned_as_oauth_error() {
        when(accounts.loginOrCreate("sub-1", "x@y.com"))
                .thenThrow(new CustomException(ExceptionCode.GOOGLE_EMAIL_CONFLICT_LOCAL));

        assertThatThrownBy(() -> service.login(Map.of("sub", "sub-1", "email", "x@y.com", "email_verified", true)))
                .isInstanceOf(OAuth2AuthenticationException.class)
                .satisfies(error -> assertThat(((OAuth2AuthenticationException) error).getError().getErrorCode())
                        .isEqualTo("GOOGLE_EMAIL_CONFLICT_LOCAL"));
    }
}
