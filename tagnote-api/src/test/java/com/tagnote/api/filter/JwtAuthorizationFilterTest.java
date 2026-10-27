package com.tagnote.api.filter;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tagnote.api.auth.PrincipalDetails;
import com.tagnote.core.domain.user.Role;
import com.tagnote.core.domain.user.UserStatus;
import com.tagnote.core.domain.user.service.User;
import com.tagnote.core.domain.user.service.UserService;
import com.tagnote.core.exception.CustomException;
import com.tagnote.core.exception.ExceptionCode;
import com.tagnote.core.utils.jwt.JwtUtil;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class JwtAuthorizationFilterTest {
    private final JwtUtil jwtUtil = new JwtUtil();
    private final UserService users = mock(UserService.class);
    private final JwtAuthorizationFilter filter = new JwtAuthorizationFilter(jwtUtil, new ObjectMapper(), users);

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(jwtUtil, "jwtKey", "test-secret");
        ReflectionTestUtils.setField(jwtUtil, "accessTokenExpiration", 3600);
        ReflectionTestUtils.setField(jwtUtil, "refreshTokenExpiration", 3600);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void access_token_restores_current_database_role() throws Exception {
        when(users.requireActiveUser(1L)).thenReturn(User.builder().userId(1L)
                .role(Role.ROLE_USER).userStatus(UserStatus.ACTIVE).build());
        MockHttpServletRequest request = request("GET", "/api/boards/auth/board-1", jwtUtil.generateAccessToken(1L));
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication().getAuthorities())
                .extracting("authority").containsExactly("ROLE_USER");
        verify(chain).doFilter(eq(request), any());
    }

    @Test
    void 인증_필터는_데이터베이스의_현재_닉네임을_인증_정보에_담는다() throws Exception {
        when(users.requireActiveUser(1L)).thenReturn(User.builder().userId(1L)
                .role(Role.ROLE_USER).userStatus(UserStatus.ACTIVE).nickname("새닉네임").build());
        MockHttpServletRequest request = request("GET", "/api/user/me", jwtUtil.generateAccessToken(1L));

        filter.doFilter(request, new MockHttpServletResponse(), mock(FilterChain.class));

        PrincipalDetails principal = (PrincipalDetails)
                SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        assertThat(principal.getNickname()).isEqualTo("새닉네임");
    }

    @Test
    void missing_token_is_anonymous() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/boards");
        FilterChain chain = mock(FilterChain.class);
        filter.doFilter(request, new MockHttpServletResponse(), chain);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chain).doFilter(eq(request), any());
    }

    @Test
    void old_pending_token_is_rejected_even_on_old_register_route() throws Exception {
        String pending = JWT.create().withSubject("pending-1").withClaim("TOKEN_TYPE", "Pending")
                .withExpiresAt(Instant.now().plusSeconds(3600)).sign(Algorithm.HMAC512("test-secret"));
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request("POST", "/api/user/register", pending), response, mock(FilterChain.class));

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("GOOGLE_RELOGIN_REQUIRED");
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verifyNoInteractions(users);
    }

    @Test
    void inactive_user_cannot_be_restored_from_access_token() throws Exception {
        when(users.requireActiveUser(1L)).thenThrow(new CustomException(ExceptionCode.USER_INACTIVE));
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request("GET", "/api/boards/auth/board-1", jwtUtil.generateAccessToken(1L)),
                response, mock(FilterChain.class));
        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void withdrawn_user_cannot_use_old_access_token() throws Exception {
        when(users.requireActiveUser(1L)).thenThrow(new CustomException(ExceptionCode.ACCOUNT_WITHDRAWN));
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);
        filter.doFilter(request("DELETE", "/api/user/me", jwtUtil.generateAccessToken(1L)), response, chain);
        assertThat(response.getStatus()).isEqualTo(401);
        verifyNoInteractions(chain);
    }

    @Test
    void refresh_token_is_rejected_as_access_token() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request("GET", "/api/boards/auth/board-1", jwtUtil.generateRefreshToken(1L)),
                response, mock(FilterChain.class));
        assertThat(response.getStatus()).isEqualTo(401);
        verifyNoInteractions(users);
    }

    @Test
    void login_ignores_stale_authorization_header() throws Exception {
        MockHttpServletRequest request = request("POST", "/api/auth/login", "stale-token");
        FilterChain chain = mock(FilterChain.class);
        filter.doFilter(request, new MockHttpServletResponse(), chain);
        verify(chain).doFilter(eq(request), any());
        verifyNoInteractions(users);
    }

    private MockHttpServletRequest request(String method, String path, String token) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        request.setServletPath(path);
        request.addHeader("Authorization", "Bearer " + token);
        return request;
    }
}
