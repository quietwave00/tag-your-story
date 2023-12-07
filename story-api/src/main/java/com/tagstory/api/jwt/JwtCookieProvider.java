package com.tagstory.api.jwt;

import org.springframework.stereotype.Component;

import javax.servlet.http.Cookie;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import static com.tagstory.core.utils.jwt.JwtProperties.*;

@Component
public class JwtCookieProvider {

    private static final String DOMAIN = "tagyourstory.blog";

    public Cookie generateAccessTokenCookie(String accessToken) {
        Cookie accessCookie = new Cookie(HEADER_STRING, encodeJwt(accessToken));
        accessCookie.setSecure(true);
        accessCookie.setPath("/");
        accessCookie.setDomain(DOMAIN);
        accessCookie.setMaxAge(10800);
        return accessCookie;
    }

    public Cookie generateRefreshTokenCookie(String refreshToken) {
        Cookie refreshCookie = new Cookie(TOKEN_TYPE_REFRESH, encodeJwt(refreshToken));
        refreshCookie.setSecure(true);
        refreshCookie.setPath("/");
        refreshCookie.setDomain(DOMAIN);
        refreshCookie.setMaxAge(86400);
        return refreshCookie;
    }

    public Cookie generatePendingUserCookie(String pendingToken) {
        Cookie pendingCookie = new Cookie(TOKEN_TYPE_PENDING, pendingToken);
        pendingCookie.setSecure(true);
        pendingCookie.setPath("/");
        pendingCookie.setDomain(DOMAIN);
        pendingCookie.setMaxAge(10800);
        return pendingCookie;
    }

    public String encodeJwt(String token) {
        return URLEncoder.encode(TOKEN_PREFIX, StandardCharsets.UTF_8) + token;
    }
}
