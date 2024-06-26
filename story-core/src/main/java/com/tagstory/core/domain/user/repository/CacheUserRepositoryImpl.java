package com.tagstory.core.domain.user.repository;

import com.tagstory.core.common.CommonRedisTemplate;
import com.tagstory.core.common.CacheSpec;
import com.tagstory.core.domain.user.service.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.Objects;

@Repository
@RequiredArgsConstructor
@Slf4j
public class CacheUserRepositoryImpl implements CacheUserRepository {

    private final CommonRedisTemplate redisTemplate;


    @Override
    public User saveCache(User user, CacheSpec cacheSpec) {
        redisTemplate.set(
                Objects.nonNull(user.getUserId()) ?
                        user.getUserId()
                        : user.getPendingUserId(),
                user,
                cacheSpec
        );
        return user;
    }

    @Override
    public void deletePendingUser(User user, CacheSpec cacheSpec) {
        redisTemplate.delete(user.getPendingUserId(), cacheSpec);
    }

    @Override
    public User findCachedUserByUserId(Long userId, CacheSpec cacheSpec) {
        return redisTemplate.get(userId, cacheSpec);
    }

    @Override
    public User findCachedUserByPendingUserId(String pendingUserId, CacheSpec cacheSpec) {
        return redisTemplate.get(pendingUserId, cacheSpec);
    }

    @Override
    public User findCachedUserByUserKey(String userKey, CacheSpec cacheSpec) {
        return redisTemplate.get(userKey, cacheSpec);
    }
}