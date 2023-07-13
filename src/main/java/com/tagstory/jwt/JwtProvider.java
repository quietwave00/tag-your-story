package com.tagstory.jwt;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.tagstory.entity.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class JwtProvider {

    @Value("${jwt.key}")
    private String jwtKey;

    @Value("${jwt.expiration}")
    private int jwtExpiration;

    public String generateToken(User user) {
        return JWT.create()
                .withSubject(user.getUserKey())
                .withExpiresAt(Instant.ofEpochSecond(jwtExpiration))
                .withClaim("userKey", user.getUserKey())
                .sign(Algorithm.HMAC512(jwtKey));
    }

}
