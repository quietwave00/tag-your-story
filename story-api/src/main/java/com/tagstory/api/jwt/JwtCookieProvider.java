package com.tagstory.api.jwt;

import org.springframework.stereotype.Component;

import javax.servlet.http.Cookie;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import static com.tagstory.core.utils.JwtProperties.*;

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

    public Cookie generatePendingUserCookie(String tempToken) {
        Cookie tempCookie = new Cookie(TOKEN_TYPE_TEMP, tempToken);
        tempCookie.setSecure(true);
        tempCookie.setPath("/");
        tempCookie.setDomain(DOMAIN);
        tempCookie.setMaxAge(10800);
        return tempCookie;
    }

    public String encodeJwt(String token) {
        return URLEncoder.encode(TOKEN_PREFIX, StandardCharsets.UTF_8) + token;
    }
}
