package com.tagnote.api.domain.auth;

import com.tagnote.core.common.CacheSpec;
import com.tagnote.core.common.CommonRedisTemplate;
import com.tagnote.core.domain.user.service.User;
import com.tagnote.core.utils.jwt.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SignupTokenService {
    private final JwtUtil jwt;
    private final CommonRedisTemplate redis;

    public Tokens issue(User user) {
        String access = jwt.generateAccessToken(user.getUserId());
        String refresh = jwt.generateRefreshToken(user.getUserId());
        redis.set(user.getUserId(), refresh, CacheSpec.REFRESH_TOKEN);
        return new Tokens(access, refresh);
    }

    public record Tokens(String access, String refresh) { }
}
