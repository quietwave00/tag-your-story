package com.tagnote.core.domain.user.repository;

import com.tagnote.core.common.CommonRedisTemplate;
import com.tagnote.core.common.CacheSpec;
import com.tagnote.core.domain.user.service.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
@Slf4j
public class CacheUserRepositoryImpl implements CacheUserRepository {

    private final CommonRedisTemplate redisTemplate;


    @Override
    public User saveCache(User user, CacheSpec cacheSpec) {
        redisTemplate.set(user.getUserId(), user, cacheSpec);
        return user;
    }

    @Override
    public User findCachedUserByUserId(Long userId, CacheSpec cacheSpec) {
        return redisTemplate.get(userId, cacheSpec);
    }

    @Override
    public User findCachedUserByUserKey(String userKey, CacheSpec cacheSpec) {
        return redisTemplate.get(userKey, cacheSpec);
    }

    @Override
    public void deleteCachedUser(Long userId, String userKey) {
        redisTemplate.delete(userId, CacheSpec.USER);
        redisTemplate.delete(userKey, CacheSpec.USER);
    }
}
