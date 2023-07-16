package com.tagstory.jwt;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.tagstory.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtUtil {

    @Value("${jwt.key}")
    private String jwtKey;

    @Value("${jwt.expiration}")
    private int jwtExpiration;



    public String generateToken(User user) {
        return JWT.create()
                .withExpiresAt(Instant.ofEpochSecond(jwtExpiration).plusSeconds(Instant.now().getEpochSecond()))
                .withClaim("userKey", user.getUserKey())
                .sign(Algorithm.HMAC512("tagStory"));
    }

    public String getUserKeyFromJwt(HttpServletRequest request) {
        String jwt = request.getHeader(JwtCookieProvider.COOKIE_NAME).replace(JwtCookieProvider.JWT_PREFIX, "");
        return JWT.require(Algorithm.HMAC512(jwtKey)).build().verify(jwt).getClaim("userKey").asString();
    }

}
