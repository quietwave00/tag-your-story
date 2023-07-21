package com.tagstory.jwt;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.servlet.http.Cookie;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
public class JwtCookieProvider {

    public final static String COOKIE_NAME = "Authorization";
    public final static String JWT_PREFIX = "Bearer ";

    public Cookie generateCookie(String jwt) {
        Cookie jwtCookie = new Cookie(COOKIE_NAME, encodeJwt(jwt));
        jwtCookie.setPath("/");
        jwtCookie.setMaxAge(10800);
        return jwtCookie;
    }

    public String encodeJwt(String jwt) {
        return URLEncoder.encode(JWT_PREFIX, StandardCharsets.UTF_8) + jwt;
    }
}
