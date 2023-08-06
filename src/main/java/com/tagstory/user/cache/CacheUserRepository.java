package com.tagstory.user.cache;

import com.tagstory.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class CacheUserRepository {
    private final RedisTemplate<String, Object> redisTemplate;

    public User save(User user) {
        redisTemplate.opsForHash().put("user", user.getUserId(), user);
        return findUserByUserId(user.getUserId()).get();
    }

    public void save(RefreshTokenData refreshTokenData) {
        redisTemplate.opsForHash().put("refreshToken", refreshTokenData.getUserId(), refreshTokenData);
    }

    public Optional<User> findUserByUserId(Long userId)  {
        return Optional.ofNullable((User) redisTemplate.opsForHash().get("user", userId));
    }


    public Optional<RefreshTokenData> findRefreshTokenByUserId(Long userId) {
        return Optional.ofNullable((RefreshTokenData) redisTemplate.opsForHash().get("refreshToken", userId));
    }

    public void updateRefreshToken(String newRefreshToken) {
        redisTemplate.opsForValue().set("refreshToken", newRefreshToken);
    }
}
