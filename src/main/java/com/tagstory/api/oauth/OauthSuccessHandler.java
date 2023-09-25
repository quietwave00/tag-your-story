package com.tagstory.api.oauth;

import com.tagstory.api.auth.PrincipalDetails;
import com.tagstory.api.jwt.JwtCookieProvider;
import com.tagstory.api.jwt.JwtUtil;
import com.tagstory.core.config.CacheSpec;
import com.tagstory.core.domain.user.redis.TagStoryRedisTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import javax.servlet.ServletException;
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
    private final TagStoryRedisTemplate redisTemplate;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws ServletException, IOException {
        log.info("OauthSuccessHandler Execute");
        PrincipalDetails principalDetails = (PrincipalDetails) authentication.getPrincipal();
        Long userId = principalDetails.getUserId();

        if(Objects.isNull(userId)) {
            String tempToken = jwtUtil.generateTempToken(principalDetails.getTempId());
            response.addCookie(jwtCookieProvider.generatePendingUserCookie(tempToken));
            getRedirectStrategy().sendRedirect(request, response, "http://localhost:5501/token.html");
        } else {
            String accessToken = jwtUtil.generateAccessToken(userId);
            String refreshToken = redisTemplate.get(userId, CacheSpec.REFRESH_TOKEN);

            if (Objects.isNull(refreshToken)) {
                refreshToken = jwtUtil.generateRefreshToken(userId);
                redisTemplate.set(userId, refreshToken, CacheSpec.REFRESH_TOKEN);
            }
            response.addCookie(jwtCookieProvider.generateAccessTokenCookie(accessToken));
            response.addCookie(jwtCookieProvider.generateRefreshTokenCookie(refreshToken));
            getRedirectStrategy().sendRedirect(request, response, "http://localhost:5501/token.html");
        }
    }
}
