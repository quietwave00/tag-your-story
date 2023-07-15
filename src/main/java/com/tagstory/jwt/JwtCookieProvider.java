package com.tagstory.jwt;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.servlet.http.Cookie;

@Component
public class JwtCookieProvider {

    private final String COOKIE_NAME = "Authorization";

    private final String JWT_PREFIX = "Bearer";

    @Value("{jwt.expiration}")
    private int cookieExpiration;



    public Cookie generateCookie(String jwt) {
        Cookie jwtCookie = new Cookie(COOKIE_NAME, JWT_PREFIX + jwt);
        jwtCookie.setPath("/");
        jwtCookie.setMaxAge(cookieExpiration);
        return jwtCookie;
    }



}
