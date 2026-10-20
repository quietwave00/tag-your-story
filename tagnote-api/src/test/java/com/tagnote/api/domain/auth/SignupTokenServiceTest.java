package com.tagnote.api.domain.auth;

import com.tagnote.core.common.CacheSpec;
import com.tagnote.core.common.CommonRedisTemplate;
import com.tagnote.core.domain.user.service.User;
import com.tagnote.core.utils.jwt.JwtUtil;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class SignupTokenServiceTest {
    @Test
    void storesRefreshTokenBeforeReturningLoginState() {
        JwtUtil jwt = mock(JwtUtil.class);
        CommonRedisTemplate redis = mock(CommonRedisTemplate.class);
        when(jwt.generateAccessToken(42L)).thenReturn("access");
        when(jwt.generateRefreshToken(42L)).thenReturn("refresh");

        SignupTokenService.Tokens tokens = new SignupTokenService(jwt, redis)
                .issue(User.builder().userId(42L).build());

        assertThat(tokens.access()).isEqualTo("access");
        assertThat(tokens.refresh()).isEqualTo("refresh");
        verify(redis).set(42L, "refresh", CacheSpec.REFRESH_TOKEN);
    }
}
