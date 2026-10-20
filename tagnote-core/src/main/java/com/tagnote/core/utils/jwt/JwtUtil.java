package com.tagnote.core.utils.jwt;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.tagnote.core.exception.CustomException;
import com.tagnote.core.exception.ExceptionCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

import static com.tagnote.core.utils.jwt.JwtProperties.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtUtil {

    @Value("${jwt.key}")
    private String jwtKey;

    @Value("${jwt.expiration}")
    private int accessTokenExpiration;

    @Value("${jwt.refresh.expiration}")
    private int refreshTokenExpiration;

//    private final JWTVerifier jwtVerifier;
//
//    public JwtUtil(@Value("${jwt.key}") String jwtKey) {
//        Algorithm algorithm = Algorithm.HMAC512(jwtKey);
//        this.jwtVerifier = JWT.require(algorithm).build();
//    }


    public String generateAccessToken(Long userId) {
        return JWT.create()
                .withExpiresAt(Instant.ofEpochSecond(accessTokenExpiration).plusSeconds(Instant.now().getEpochSecond()))
                .withClaim("TOKEN_TYPE", TOKEN_TYPE_ACCESS)
                .withSubject(String.valueOf(userId))
                .sign(Algorithm.HMAC512(jwtKey));
    }

    public String generateRefreshToken(Long userId) {
        return JWT.create()
                .withExpiresAt(Instant.ofEpochSecond(refreshTokenExpiration).plusSeconds(Instant.now().getEpochSecond()))
                .withClaim("TOKEN_TYPE", TOKEN_TYPE_REFRESH)
                .withSubject(String.valueOf(userId))
                .withJWTId(UUID.randomUUID().toString())
                .sign(Algorithm.HMAC512(jwtKey));
    }

    public Long getUserIdFromToken(String token, String expectedType) {
        DecodedJWT decoded = validateToken(token);
        if ("Pending".equals(decoded.getClaim("TOKEN_TYPE").asString())) {
            throw new CustomException(ExceptionCode.GOOGLE_RELOGIN_REQUIRED);
        }
        if (!expectedType.equals(decoded.getClaim("TOKEN_TYPE").asString())) {
            throw new CustomException(ExceptionCode.INVALID_REFRESH_TOKEN);
        }
        try {
            return Long.valueOf(decoded.getSubject());
        } catch (NumberFormatException | NullPointerException e) {
            throw new CustomException(ExceptionCode.INVALID_REFRESH_TOKEN);
        }
    }


    public DecodedJWT validateToken(String token) {
        try {
            JWTVerifier verifier = JWT.require(Algorithm.HMAC512(jwtKey)).build();
            return verifier.verify(token);
        } catch (TokenExpiredException e) {
            throw new CustomException(ExceptionCode.TOKEN_HAS_EXPIRED);
        } catch (JWTVerificationException | IllegalArgumentException e) {
            throw new CustomException(ExceptionCode.TOKEN_HAS_TEMPERED);
        }
    }
}
