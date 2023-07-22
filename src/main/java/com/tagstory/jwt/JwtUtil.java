package com.tagstory.jwt;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.*;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.tagstory.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.Date;

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

    public String getUserKeyFromJwt(HttpServletRequest request) {
        String jwt = request.getHeader("Authorization").replace("Bearer", "");
        return JWT.require(Algorithm.HMAC512(jwtKey)).build().verify(jwt).getClaim("userKey").asString();
    }

    public String getUserKeyFromRefreshToken(HttpServletRequest request) {
        String refreshToken = request.getHeader("RefreshToken").replace("Bearer", "");
        return JWT.require(Algorithm.HMAC512(jwtKey)).build().verify(refreshToken).getClaim("userKey").asString();
    }

    public void validateJwt(String jwt) {
        try {
            JWTVerifier verifier = JWT.require(Algorithm.HMAC512(jwtKey)).build();
            DecodedJWT decodedJWT = verifier.verify(jwt);
        } catch (TokenExpiredException e) {

        } catch (AlgorithmMismatchException | SignatureVerificationException e) { //위조

        }
    }

}
