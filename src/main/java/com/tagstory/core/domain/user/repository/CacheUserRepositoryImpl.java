package com.tagstory.core.domain.user.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tagstory.api.exception.CustomException;
import com.tagstory.api.exception.ExceptionCode;
import com.tagstory.core.config.CacheSpec;
import com.tagstory.core.domain.user.service.dto.response.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.Objects;

@Repository
@RequiredArgsConstructor
@Slf4j
public class CacheUserRepositoryImpl implements CacheUserRepository {
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public User saveCache(User user, CacheSpec cacheSpec) {
        try {
            String json = objectMapper.writeValueAsString(user);
            if(Objects.nonNull(user.getUserId())) {
                redisTemplate.opsForValue().set(cacheSpec.generateKey(user.getUserId()), json);
            } else {
                redisTemplate.opsForValue().set(cacheSpec.generateKey(user.getPendingUserId()), json);
            }
            return user;
        } catch (JsonProcessingException e) {
            throw new CustomException(ExceptionCode.CONVERSION_EXCEPTION);
        }
    }

    @Override
    public boolean deleteCache(User user, CacheSpec cacheSpec) {
        try {
            String key = cacheSpec.generateKey(user.getPendingUserId());
            return redisTemplate.delete(key);
        } catch (NullPointerException e) {
            throw new CustomException(ExceptionCode.CACHE_DELETE_EXCEPTION);
        }
    }

    @Override
    public User findCachedUserByUserId(Long userId, CacheSpec cacheSpec) {
        String json = redisTemplate.opsForValue().get(cacheSpec.generateKey(userId));
        if (json == null) return null;
        try {
            return objectMapper.readValue(json, cacheSpec.getClazz());
        } catch (JsonProcessingException e) {
            throw new CustomException(ExceptionCode.CONVERSION_EXCEPTION);
        }
    }

    @Override
    public User findCachedUserByPendingUserId(String pendingUserId, CacheSpec cacheSpec) {
        String json = redisTemplate.opsForValue().get(cacheSpec.generateKey(pendingUserId));
        try {
            return objectMapper.readValue(json, cacheSpec.getClazz());
        } catch (JsonProcessingException e) {
            throw new CustomException(ExceptionCode.CONVERSION_EXCEPTION);
        }
    }

    @Override
    public User findCachedUserByUserKey(String userKey, CacheSpec cacheSpec) {
        String json = redisTemplate.opsForValue().get(cacheSpec.generateKey(userKey));
        try {
            return objectMapper.readValue(json, cacheSpec.getClazz());
        } catch (JsonProcessingException e) {
            throw new CustomException(ExceptionCode.CONVERSION_EXCEPTION);
        }
    }
}