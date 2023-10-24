package com.tagstory.core.utils;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.AlgorithmMismatchException;
import com.auth0.jwt.exceptions.SignatureVerificationException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.tagstory.core.exception.CustomException;
import com.tagstory.core.exception.ExceptionCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;

import static com.tagstory.core.utils.JwtProperties.*;

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
                .sign(Algorithm.HMAC512(jwtKey));
    }

    public String generateTempToken(String tempId) {
        return JWT.create()
                .withExpiresAt(Instant.ofEpochSecond(accessTokenExpiration).plusSeconds(Instant.now().getEpochSecond()))
                .withClaim("TOKEN_TYPE", TOKEN_TYPE_TEMP)
                .withSubject(tempId)
                .sign(Algorithm.HMAC512(jwtKey));
    }

    public Long getUserIdFromToken(String token) throws CustomException {
        return Long.valueOf(validateToken(token).getSubject());
    }

    public String getTempIdFromToken(String token) throws CustomException {
        return validateToken(token).getSubject();
    }


    public String getTokenTypeFromToken(String token) {
        return validateToken(token).getClaim("TOKEN_TYPE").asString();
    }

    public DecodedJWT validateToken(String token) {
        try {
            JWTVerifier verifier = JWT.require(Algorithm.HMAC512(jwtKey)).build();
            return verifier.verify(token);
        } catch (TokenExpiredException e) {
            throw new CustomException(ExceptionCode.TOKEN_HAS_EXPIRED);
        } catch (AlgorithmMismatchException | SignatureVerificationException e) {
            throw new CustomException(ExceptionCode.TOKEN_HAS_TEMPERED);
        }
    }
}
