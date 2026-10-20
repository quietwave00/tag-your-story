package com.tagnote.api.oauth;

import com.tagnote.api.auth.PrincipalDetails;
import com.tagnote.api.jwt.JwtCookieProvider;
import com.tagnote.core.common.CommonRedisTemplate;
import com.tagnote.core.common.CacheSpec;
import com.tagnote.core.domain.user.Role;
import com.tagnote.core.domain.user.service.User;
import com.tagnote.core.domain.user.service.UserService;
import com.tagnote.core.exception.CustomException;
import com.tagnote.core.exception.ExceptionCode;
import com.tagnote.core.utils.jwt.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.util.ReflectionTestUtils;

import jakarta.servlet.http.Cookie;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class OauthSuccessHandlerTest {
    private final JwtCookieProvider cookies = mock(JwtCookieProvider.class);
    private final JwtUtil jwt = mock(JwtUtil.class);
    private final CommonRedisTemplate redis = mock(CommonRedisTemplate.class);
    private final UserService users = mock(UserService.class);
    private final OauthFailureHandler failures = mock(OauthFailureHandler.class);
    private final OauthSuccessHandler handler = new OauthSuccessHandler(cookies, jwt, redis, users, failures);

    @Test
    void active_account_receives_user_tokens_after_database_status_check() throws Exception {
        ReflectionTestUtils.setField(handler, "REDIRECT_URL", "http://localhost/token.html");
        when(users.requireActiveUser(7L)).thenReturn(User.builder().userId(7L).role(Role.ROLE_USER).build());
        when(jwt.generateAccessToken(7L)).thenReturn("access");
        when(jwt.generateRefreshToken(7L)).thenReturn("refresh");
        when(cookies.generateAccessTokenCookie("access")).thenReturn(new Cookie("Authorization", "access"));
        when(cookies.generateRefreshTokenCookie("refresh")).thenReturn(new Cookie("RefreshToken", "refresh"));
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationSuccess(new MockHttpServletRequest(), response,
                new UsernamePasswordAuthenticationToken(new PrincipalDetails(7L, Role.ROLE_USER), null));

        assertThat(response.getStatus()).isEqualTo(302);
        assertThat(response.getCookies()).extracting(Cookie::getName)
                .containsExactly("Authorization", "RefreshToken");
        verify(redis).set(7L, "refresh", CacheSpec.REFRESH_TOKEN);
    }

    @Test
    void account_disabled_before_token_issuance_receives_no_cookie() throws Exception {
        when(users.requireActiveUser(7L)).thenThrow(new CustomException(ExceptionCode.USER_INACTIVE));
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationSuccess(new MockHttpServletRequest(), response,
                new UsernamePasswordAuthenticationToken(new PrincipalDetails(7L, Role.ROLE_USER), null));

        assertThat(response.getCookies()).isEmpty();
        verifyNoInteractions(jwt, redis, cookies);
        verify(failures).onAuthenticationFailure(any(), eq(response), any());
    }
}
