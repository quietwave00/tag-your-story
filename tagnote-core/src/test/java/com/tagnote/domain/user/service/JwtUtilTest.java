package com.tagnote.domain.user.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.tagnote.core.exception.CustomException;
import com.tagnote.core.exception.ExceptionCode;
import com.tagnote.core.utils.jwt.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;

import static com.tagnote.core.utils.jwt.JwtProperties.TOKEN_TYPE_ACCESS;
import static com.tagnote.core.utils.jwt.JwtProperties.TOKEN_TYPE_REFRESH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtUtilTest {
    private final JwtUtil jwtUtil = new JwtUtil();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(jwtUtil, "jwtKey", "test-secret");
        ReflectionTestUtils.setField(jwtUtil, "accessTokenExpiration", 3600);
        ReflectionTestUtils.setField(jwtUtil, "refreshTokenExpiration", 3600);
    }

    @Test
    void access_타입_검증은_refresh와_타입_누락을_거부한다() {
        String refresh = jwtUtil.generateRefreshToken(1L);
        String missingType = JWT.create().withSubject("1")
                .withExpiresAt(Instant.now().plusSeconds(3600))
                .sign(Algorithm.HMAC512("test-secret"));

        assertThatThrownBy(() -> jwtUtil.getUserIdFromToken(refresh, TOKEN_TYPE_ACCESS))
                .isInstanceOf(CustomException.class)
                .satisfies(error -> assertThat(((CustomException) error).getExceptionCode())
                        .isEqualTo(ExceptionCode.INVALID_REFRESH_TOKEN));
        assertThatThrownBy(() -> jwtUtil.getUserIdFromToken(missingType, TOKEN_TYPE_ACCESS))
                .isInstanceOf(CustomException.class);
    }

    @Test
    void refresh_토큰은_같은_초에_갱신해도_다른_값이다() {
        String first = jwtUtil.generateRefreshToken(1L);
        String second = jwtUtil.generateRefreshToken(1L);

        assertThat(second).isNotEqualTo(first);
        assertThat(jwtUtil.getUserIdFromToken(second, TOKEN_TYPE_REFRESH)).isEqualTo(1L);
    }

    @Test
    void old_pending_token_requires_google_relogin() {
        String pending = JWT.create().withSubject("pending-1").withClaim("TOKEN_TYPE", "Pending")
                .withExpiresAt(Instant.now().plusSeconds(3600))
                .sign(Algorithm.HMAC512("test-secret"));

        assertThatThrownBy(() -> jwtUtil.getUserIdFromToken(pending, TOKEN_TYPE_REFRESH))
                .isInstanceOf(CustomException.class)
                .satisfies(error -> assertThat(((CustomException) error).getExceptionCode())
                        .isEqualTo(ExceptionCode.GOOGLE_RELOGIN_REQUIRED));
    }

    @Test
    void 위조_만료_잘못된_subject는_401_예외이다() {
        String forged = JWT.create().withSubject("1").withClaim("TOKEN_TYPE", TOKEN_TYPE_REFRESH)
                .withExpiresAt(Instant.now().plusSeconds(3600)).sign(Algorithm.HMAC512("other-secret"));
        String expired = JWT.create().withSubject("1").withClaim("TOKEN_TYPE", TOKEN_TYPE_REFRESH)
                .withExpiresAt(Instant.now().minusSeconds(1)).sign(Algorithm.HMAC512("test-secret"));
        String invalidSubject = JWT.create().withSubject("not-a-number").withClaim("TOKEN_TYPE", TOKEN_TYPE_REFRESH)
                .withExpiresAt(Instant.now().plusSeconds(3600)).sign(Algorithm.HMAC512("test-secret"));

        assertThatThrownBy(() -> jwtUtil.getUserIdFromToken(forged, TOKEN_TYPE_REFRESH))
                .isInstanceOf(CustomException.class);
        assertThatThrownBy(() -> jwtUtil.getUserIdFromToken(expired, TOKEN_TYPE_REFRESH))
                .isInstanceOf(CustomException.class);
        assertThatThrownBy(() -> jwtUtil.getUserIdFromToken(invalidSubject, TOKEN_TYPE_REFRESH))
                .isInstanceOf(CustomException.class);
    }
}
