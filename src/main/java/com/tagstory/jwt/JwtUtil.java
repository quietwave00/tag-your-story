package com.tagstory.jwt;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.AlgorithmMismatchException;
import com.auth0.jwt.exceptions.SignatureVerificationException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.tagstory.exception.CustomException;
import com.tagstory.exception.ExceptionCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtUtil {

    @Value("${jwt.key}")
    private String jwtKey;

    @Value("${jwt.expiration}")
    private int jwtExpiration;

    @Value("${jwt.refresh.expiration}")
    private int refreshExpiration;


    public String generateAccessToken(Long userId) {
        return JWT.create()
                .withExpiresAt(Instant.ofEpochSecond(jwtExpiration).plusSeconds(Instant.now().getEpochSecond()))
                .withSubject(String.valueOf(userId))
                .sign(Algorithm.HMAC512(jwtKey));
    }

    public String generateRefreshToken(Long userId) {
        return JWT.create()
                .withExpiresAt(Instant.ofEpochSecond(refreshExpiration).plusSeconds(Instant.now().getEpochSecond()))
                .withClaim("userId", userId)
                .sign(Algorithm.HMAC512(jwtKey));
    }

    public Long getUserIdFromAccessToken(String accessToken) throws CustomException {
        return Long.valueOf(validateToken(accessToken).getSubject());
    }

    public Long getUserIdFromRefreshToken(String refreshToken) throws CustomException {
        return Long.valueOf(validateToken(refreshToken).getSubject());
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
