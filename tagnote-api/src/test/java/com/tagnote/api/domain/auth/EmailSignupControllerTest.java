package com.tagnote.api.domain.auth;

import com.tagnote.api.jwt.JwtCookieProvider;
import com.tagnote.api.support.WebMvcMethodSecurityTestConfig;
import com.tagnote.application.auth.EmailSignupService;
import com.tagnote.core.domain.user.AccountProvider;
import com.tagnote.core.domain.user.Role;
import com.tagnote.core.domain.user.UserStatus;
import com.tagnote.core.domain.user.service.User;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(EmailSignupController.class)
@Import(WebMvcMethodSecurityTestConfig.class)
class EmailSignupControllerTest {
    @Autowired MockMvc mvc;
    @MockBean EmailSignupService signup;
    @MockBean SignupTokenService tokens;
    @MockBean JwtCookieProvider cookies;

    @Test
    void startReturnsChallengeIdWithoutIssuingTokens() throws Exception {
        when(signup.start("user@example.com", "Password123", "Password123", "127.0.0.1")).thenReturn("challenge-id");
        mvc.perform(post("/api/auth/signup/email/start").contentType(APPLICATION_JSON)
                        .content("{\"email\":\"user@example.com\",\"password\":\"Password123\",\"passwordConfirm\":\"Password123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.response.challengeId").value("challenge-id"));
        verifyNoInteractions(tokens, cookies);
    }

    @Test
    void missingConfirmationIsRejected() throws Exception {
        mvc.perform(post("/api/auth/signup/email/start").contentType(APPLICATION_JSON)
                        .content("{\"email\":\"user@example.com\",\"password\":\"Password123\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(signup);
    }

    @Test
    void confirmIssuesCookiesOnlyForSavedUser() throws Exception {
        User user = User.builder().userId(42L).email("user@example.com").nickname("푸른여우4821")
                .provider(AccountProvider.LOCAL).role(Role.ROLE_USER).userStatus(UserStatus.ACTIVE).build();
        when(signup.confirm("challenge-id", "123456")).thenReturn(user);
        when(tokens.issue(user)).thenReturn(new SignupTokenService.Tokens("access", "refresh"));
        when(cookies.generateAccessTokenCookie("access")).thenReturn(new Cookie("Authorization", "access"));
        when(cookies.generateRefreshTokenCookie("refresh")).thenReturn(new Cookie("Refresh", "refresh"));
        mvc.perform(post("/api/auth/signup/email/confirm").contentType(APPLICATION_JSON)
                        .content("{\"challengeId\":\"challenge-id\",\"code\":\"123456\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.response.userId").value(42))
                .andExpect(jsonPath("$.response.nickname").value("푸른여우4821"))
                .andExpect(cookie().exists("Authorization"))
                .andExpect(cookie().exists("Refresh"));
        verify(tokens).issue(user);
    }

    @Test
    void availabilityUsesQueryEmail() throws Exception {
        when(signup.available("User@Example.com", "127.0.0.1")).thenReturn(true);
        mvc.perform(get("/api/auth/email-availability").param("email", "User@Example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.response.available").value(true));
    }
}
