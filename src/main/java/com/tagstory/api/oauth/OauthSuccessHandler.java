package com.tagstory.api.oauth;

import com.tagstory.api.auth.PrincipalDetails;
import com.tagstory.api.jwt.JwtCookieProvider;
import com.tagstory.api.jwt.JwtUtil;
import com.tagstory.core.common.CommonRedisTemplate;
import com.tagstory.core.config.CacheSpec;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Objects;

@Slf4j
@Component
@RequiredArgsConstructor
public class OauthSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

    private final JwtCookieProvider jwtCookieProvider;
    private final JwtUtil jwtUtil;
    private final CommonRedisTemplate redisTemplate;

    @Value("${api.redirect-url}")
    private String REDIRECT_URL;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {
        log.info("OauthSuccessHandler Execute");
        PrincipalDetails principalDetails = (PrincipalDetails) authentication.getPrincipal();
        Long userId = principalDetails.getUserId();
        log.info("OatuhSuccessHandler userId: {}", userId);

        if(Objects.isNull(userId)) {
            log.info("pendingUser");
            String tempToken = jwtUtil.generateTempToken(principalDetails.getTempId());
            response.addCookie(jwtCookieProvider.generatePendingUserCookie(tempToken));
            getRedirectStrategy().sendRedirect(request, response, REDIRECT_URL);
        } else {
            log.info("registered user");
            String accessToken = jwtUtil.generateAccessToken(userId);
            String refreshToken = redisTemplate.get(userId, CacheSpec.REFRESH_TOKEN);

            if (Objects.isNull(refreshToken)) {
                refreshToken = jwtUtil.generateRefreshToken(userId);
                redisTemplate.set(userId, refreshToken, CacheSpec.REFRESH_TOKEN);
            }
            response.addCookie(jwtCookieProvider.generateAccessTokenCookie(accessToken));
            response.addCookie(jwtCookieProvider.generateRefreshTokenCookie(refreshToken));
            getRedirectStrategy().sendRedirect(request, response, REDIRECT_URL);
        }
    }
}
