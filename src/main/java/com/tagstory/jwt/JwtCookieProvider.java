package com.tagstory.jwt;

import org.springframework.stereotype.Component;

import javax.servlet.http.Cookie;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
public class JwtCookieProvider {

    public final static String JWT_COOKIE = "Authorization";
    public final static String REFRESH_COOKIE = "RefreshToken";
    public final static String TOKEN_PREFIX = "Bearer ";

    public Cookie generateJwtCookie(String jwt) {
        Cookie jwtCookie = new Cookie(JWT_COOKIE, encodeJwt(jwt));
        jwtCookie.setPath("/");
        jwtCookie.setMaxAge(10800);
        return jwtCookie;
    }

    public Cookie generateRefreshTokenCookie(String refreshToken) {
        Cookie refreshCookie = new Cookie(REFRESH_COOKIE, encodeJwt(refreshToken));
        refreshCookie.setPath("/");
        refreshCookie.setMaxAge(86400);
        return refreshCookie;
    }

    public String encodeJwt(String token) {
        return URLEncoder.encode(TOKEN_PREFIX, StandardCharsets.UTF_8) + token;
    }
}
