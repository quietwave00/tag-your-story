package com.tagstory.oauth;

import com.mysql.cj.util.StringUtils;
import com.tagstory.auth.PrincipalDetails;
import com.tagstory.entity.User;
import com.tagstory.jwt.JwtCookieProvider;
import com.tagstory.jwt.JwtUtil;
import com.tagstory.user.cache.CacheUserRepository;
import com.tagstory.user.cache.RefreshTokenData;
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
    private final CacheUserRepository cacheUserRepository;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws ServletException, IOException {
        log.info("OauthSuccessHandler Execute");
        PrincipalDetails principalDetails = (PrincipalDetails) authentication.getPrincipal();
        User user = principalDetails.getUser();
        String accessToken = jwtUtil.generateAccessToken(user.getUserId());
        String refreshToken = cacheUserRepository.findRefreshTokenByUserId(user.getUserId())
                .map(RefreshTokenData::getRefreshToken)
                .orElse(null);

        if(StringUtils.isNullOrEmpty(refreshToken)) {
            refreshToken = jwtUtil.generateRefreshToken(user.getUserId());
            saveRefreshToken(user.getUserId(), refreshToken);
        }

        response.addCookie(jwtCookieProvider.generateAccessTokenCookie(accessToken));
        response.addCookie(jwtCookieProvider.generateRefreshTokenCookie(refreshToken));
        getRedirectStrategy().sendRedirect(request, response, "http://localhost:5500/html/user/token.html");
    }

    private void saveRefreshToken(Long userId, String refreshToken) {
        RefreshTokenData refreshTokenData = RefreshTokenData.builder()
                                    .userId(userId)
                                    .refreshToken(refreshToken)
                                    .build();
        cacheUserRepository.save(refreshTokenData);
    }
}
