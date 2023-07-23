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
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtUtil {

    @Value("${jwt.key}")
    private String jwtKey;


    public String generateAccessToken(User user) {
        return JWT.create()
                .withExpiresAt(Instant.ofEpochSecond(10800).plusSeconds(Instant.now().getEpochSecond()))
                .withClaim("userId", user.getUserId())
                .sign(Algorithm.HMAC512(jwtKey));
    }

    public String generateRefreshToken(String userKey) {
        return JWT.create()
                .withExpiresAt(Instant.ofEpochSecond(86400).plusSeconds(Instant.now().getEpochSecond()))
                .withClaim("userKey", userKey)
                .sign(Algorithm.HMAC512(jwtKey));
    }

    public Long getUserIdFromJwt(HttpServletRequest request) throws CustomException {
        String jwt = request.getHeader("Authorization").replace("Bearer ", "");
        return validateJwt(jwt).getClaim("userId").asLong();
    }

    public Long getUserIdFromRefreshToken(HttpServletRequest request) throws CustomException {
        String refreshToken = request.getHeader("RefreshToken").replace("Bearer ", "");
        return validateJwt(refreshToken).getClaim("userId").asLong();
    }

    public DecodedJWT validateJwt(String jwt) {
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
