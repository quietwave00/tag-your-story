package com.tagstory.api.jwt;

import org.springframework.stereotype.Component;

import javax.servlet.http.Cookie;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import static com.tagstory.core.utils.JwtProperties.*;

@Component
public class JwtCookieProvider {

    public Cookie generateAccessTokenCookie(String jwt) {
        Cookie accessCookie = new Cookie(HEADER_STRING, encodeJwt(jwt));
        accessCookie.setPath("/");
        accessCookie.setMaxAge(10800);
        return accessCookie;
    }

    public Cookie generateRefreshTokenCookie(String refreshToken) {
        Cookie refreshCookie = new Cookie(TOKEN_TYPE_REFRESH, encodeJwt(refreshToken));
        refreshCookie.setPath("/");
        refreshCookie.setMaxAge(86400);
        return refreshCookie;
    }

    public Cookie generatePendingUserCookie(String tempToken) {
        Cookie tempCookie = new Cookie(TOKEN_TYPE_TEMP, tempToken);
        tempCookie.setPath("/");
        tempCookie.setMaxAge(10800);
        return tempCookie;
    }

    public String encodeJwt(String token) {
        return URLEncoder.encode(TOKEN_PREFIX, StandardCharsets.UTF_8) + token;
    }
}
