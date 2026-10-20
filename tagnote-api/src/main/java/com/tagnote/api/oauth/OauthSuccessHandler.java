package com.tagnote.api.oauth;

import com.tagnote.api.auth.PrincipalDetails;
import com.tagnote.api.jwt.JwtCookieProvider;
import com.tagnote.core.common.CommonRedisTemplate;
import com.tagnote.core.common.CacheSpec;
import com.tagnote.core.domain.user.service.UserService;
import com.tagnote.core.exception.CustomException;
import com.tagnote.core.utils.jwt.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class OauthSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

    private final JwtCookieProvider jwtCookieProvider;
    private final JwtUtil jwtUtil;
    private final CommonRedisTemplate redisTemplate;
    private final UserService userService;
    private final OauthFailureHandler failureHandler;

    @Value("${api.redirect-url}")
    private String REDIRECT_URL;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {
        PrincipalDetails principalDetails = (PrincipalDetails) authentication.getPrincipal();
        Long userId = principalDetails.getUserId();

        try {
            userService.requireActiveUser(userId);
        } catch (CustomException e) {
            failureHandler.onAuthenticationFailure(request, response,
                    new OAuth2AuthenticationException(new OAuth2Error("USER_INACTIVE")));
            return;
        }
        String accessToken = jwtUtil.generateAccessToken(userId);
        String refreshToken = jwtUtil.generateRefreshToken(userId);
        redisTemplate.set(userId, refreshToken, CacheSpec.REFRESH_TOKEN);

        response.addCookie(jwtCookieProvider.generateAccessTokenCookie(accessToken));
        response.addCookie(jwtCookieProvider.generateRefreshTokenCookie(refreshToken));
        getRedirectStrategy().sendRedirect(request, response, REDIRECT_URL);
    }

}
