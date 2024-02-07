package com.tagstory.api.oauth;

import com.tagstory.api.auth.PrincipalDetails;
import com.tagstory.api.jwt.JwtCookieProvider;
import com.tagstory.core.common.CommonRedisTemplate;
import com.tagstory.core.common.CacheSpec;
import com.tagstory.core.domain.user.repository.UserRepository;
import com.tagstory.core.domain.user.service.User;
import com.tagstory.core.domain.user.service.UserService;
import com.tagstory.core.utils.jwt.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Objects;

@Slf4j
@Component
@RequiredArgsConstructor
public class OauthSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

    private final JwtCookieProvider jwtCookieProvider;
    private final JwtUtil jwtUtil;
    private final CommonRedisTemplate redisTemplate;
    private final UserService userService;

    @Value("${api.redirect-url}")
    private String REDIRECT_URL;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {
        PrincipalDetails principalDetails = (PrincipalDetails) authentication.getPrincipal();
        Long userId = principalDetails.getUserId();

        /* userId가 없으면 Pending 타입의 토큰을 발급한다. */
        if(Objects.isNull(userId)) {
            String pendingToken = jwtUtil.generatePendingToken(principalDetails.getPendingId());
            response.addCookie(jwtCookieProvider.generatePendingUserCookie(pendingToken));
            getRedirectStrategy().sendRedirect(request, response, REDIRECT_URL);
        } else {
            /* 로그인 성공 시 유저 정보가 캐싱되어 있는지 체크한다. */
            hasCachedUserInfo(userId);

            /* userId가 있으면 AccessToken과 RefreshToken을 발급한다. */
            String accessToken = jwtUtil.generateAccessToken(userId);
            String refreshToken = jwtUtil.generateRefreshToken(userId);
            redisTemplate.set(userId, refreshToken, CacheSpec.REFRESH_TOKEN);

            response.addCookie(jwtCookieProvider.generateAccessTokenCookie(accessToken));
            response.addCookie(jwtCookieProvider.generateRefreshTokenCookie(refreshToken));
            getRedirectStrategy().sendRedirect(request, response, REDIRECT_URL);
        }
    }

    /**
     * 유저 정보가 캐싱되어 있는지 확인한다.
     *
     * @param userId
     */
    private void hasCachedUserInfo(Long userId) {
        boolean hasCachedUserInfo = userService.hasCachedUserInfo(userId);
        if(!hasCachedUserInfo) {
            User user = userService.findByUserId(userId);
            userService.saveCache(user.toEntity());
        }
    }
}
