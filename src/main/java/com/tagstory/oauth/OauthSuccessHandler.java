package com.tagstory.oauth;

import com.tagstory.auth.PrincipalDetails;
import com.tagstory.entity.User;
import com.tagstory.jwt.JwtCookieProvider;
import com.tagstory.jwt.JwtUtil;
import com.tagstory.user.cache.CacheSpec;
import com.tagstory.user.cache.TagStoryRedisTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

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
        User user = principalDetails.getUser();
        String accessToken = jwtUtil.generateAccessToken(user.getUserId());
        String refreshToken = redisTemplate.get(user.getUserId(), CacheSpec.REFRESH_TOKEN);

        if (refreshToken == null) {
            refreshToken = jwtUtil.generateRefreshToken(user.getUserId());
            redisTemplate.set(user.getUserId(), refreshToken, CacheSpec.REFRESH_TOKEN);
        }

        response.addCookie(jwtCookieProvider.generateAccessTokenCookie(accessToken));
        response.addCookie(jwtCookieProvider.generateRefreshTokenCookie(refreshToken));
        getRedirectStrategy().sendRedirect(request, response, "http://localhost:5500/html/user/token.html");
    }
}
