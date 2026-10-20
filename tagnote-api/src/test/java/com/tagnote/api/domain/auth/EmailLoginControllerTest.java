package com.tagnote.api.domain.auth;

import com.tagnote.api.jwt.JwtCookieProvider;
import com.tagnote.api.support.WebMvcMethodSecurityTestConfig;
import com.tagnote.application.auth.EmailLoginService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(EmailLoginController.class)
@Import(WebMvcMethodSecurityTestConfig.class)
class EmailLoginControllerTest {
    @Autowired MockMvc mvc;
    @MockBean EmailLoginService login;
    @MockBean SignupTokenService tokens;
    @MockBean JwtCookieProvider cookies;

    @Test
    void successUsesExistingTokenCookies() throws Exception {
        User user = User.builder().userId(42L).nickname("푸른여우4821").build();
        when(login.login(" User@Example.com ", "Password123", "127.0.0.1")).thenReturn(user);
        when(tokens.issue(user)).thenReturn(new SignupTokenService.Tokens("access", "refresh"));
        when(cookies.generateAccessTokenCookie("access")).thenReturn(new Cookie("Authorization", "access"));
        when(cookies.generateRefreshTokenCookie("refresh")).thenReturn(new Cookie("Refresh", "refresh"));
        mvc.perform(post("/api/auth/login").contentType(APPLICATION_JSON)
                        .content("{\"email\":\" User@Example.com \",\"password\":\"Password123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.response.userId").value(42))
                .andExpect(cookie().exists("Authorization"))
                .andExpect(cookie().exists("Refresh"));
        verify(tokens).issue(user);
    }

    @Test
    void missingPasswordDoesNotIssueTokens() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(APPLICATION_JSON)
                        .content("{\"email\":\"user@example.com\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(login, tokens, cookies);
    }
}
