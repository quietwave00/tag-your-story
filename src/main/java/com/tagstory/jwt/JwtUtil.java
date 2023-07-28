package com.tagstory.jwt;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.*;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.tagstory.entity.User;
import com.tagstory.exception.CustomException;
import com.tagstory.exception.ExceptionCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
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

    @Value("${jwt.refresh.expiration}")
    private int refreshExpiration;


    public String generateAccessToken(Long userId) {
        return JWT.create()
                .withExpiresAt(Instant.ofEpochSecond(jwtExpiration).plusSeconds(Instant.now().getEpochSecond()))
                .withClaim("userId", userId)
                .sign(Algorithm.HMAC512(jwtKey));
    }

    public String generateRefreshToken(String userKey) {
        return JWT.create()
                .withExpiresAt(Instant.ofEpochSecond(refreshExpiration).plusSeconds(Instant.now().getEpochSecond()))
                .withClaim("userKey", userKey)
                .sign(Algorithm.HMAC512(jwtKey));
    }

    public Long getUserIdFromJwt(HttpServletRequest request) throws CustomException {
        String jwt = request.getHeader("Authorization").replace("Bearer ", "");
        return validateToken(jwt).getClaim("userId").asLong();
    }

    public Long getUserIdFromRefreshToken(HttpServletRequest request) throws CustomException {
        String refreshToken = request.getHeader("RefreshToken").replace("Bearer ", "");
        return validateToken(refreshToken).getClaim("userId").asLong();
    }

    public DecodedJWT validateToken(String jwt) {
        try {
            JWTVerifier verifier = JWT.require(Algorithm.HMAC512(jwtKey)).build();
            return verifier.verify(jwt);
        } catch (TokenExpiredException e) {
            throw new CustomException(ExceptionCode.TOKEN_HAS_EXPIRED);
        } catch (AlgorithmMismatchException | SignatureVerificationException e) {
            throw new CustomException(ExceptionCode.TOKEN_HAS_TEMPERED);
        }
    }

}
