package com.tagstory.api.jwt;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.Cookie;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import static com.tagstory.core.utils.jwt.JwtProperties.*;

@Component
public class JwtCookieProvider {

    @Value("${api.domain}")
    private String DOMAIN;

    public Cookie generateAccessTokenCookie(String accessToken) {
        Cookie accessCookie = new Cookie(HEADER_STRING, encodeJwt(accessToken));
        accessCookie.setPath("/");
        accessCookie.setDomain(DOMAIN);
        return accessCookie;
    }

    public Cookie generateRefreshTokenCookie(String refreshToken) {
        Cookie refreshCookie = new Cookie(TOKEN_TYPE_REFRESH, encodeJwt(refreshToken));
        refreshCookie.setPath("/");
        refreshCookie.setDomain(DOMAIN);
        return refreshCookie;
    }

    public Cookie generatePendingUserCookie(String pendingToken) {
        Cookie pendingCookie = new Cookie(TOKEN_TYPE_PENDING, pendingToken);
        pendingCookie.setHttpOnly(false);
        pendingCookie.setPath("/");
        pendingCookie.setDomain(DOMAIN);
        return pendingCookie;
    }

    public String encodeJwt(String token) {
        return URLEncoder.encode(TOKEN_PREFIX, StandardCharsets.UTF_8) + token;
    }
}
