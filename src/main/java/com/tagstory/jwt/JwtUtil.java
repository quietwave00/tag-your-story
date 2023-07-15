package com.tagstory.jwt;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.tagstory.entity.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Date;

@Component
public class JwtUtil {

    @Value("${jwt.key}")
    private String jwtKey;

    @Value("${jwt.expiration}")
    private int jwtExpiration;

    public String generateToken(User user) {
        return JWT.create()
                .withExpiresAt(Instant.ofEpochSecond(jwtExpiration).plusSeconds(Instant.now().getEpochSecond()))
                .withClaim("userKey", user.getUserKey())
                .sign(Algorithm.HMAC512(jwtKey));
    }

}
