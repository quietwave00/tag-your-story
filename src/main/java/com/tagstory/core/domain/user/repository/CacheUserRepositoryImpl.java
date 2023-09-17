package com.tagstory.core.domain.user.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tagstory.api.exception.CustomException;
import com.tagstory.api.exception.ExceptionCode;
import com.tagstory.core.config.CacheSpec;
import com.tagstory.core.domain.user.repository.dto.CacheUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
@Slf4j
public class CacheUserRepositoryImpl implements CacheUserRepository {
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public void saveCache(CacheUser cacheUser, CacheSpec cacheSpec) {
        try {
            String jsonString = objectMapper.writeValueAsString(cacheUser);
            redisTemplate.opsForValue().set(cacheSpec.generateKey(cacheUser.getUserId()), jsonString);
        } catch (JsonProcessingException e) {
            log.error(e.getMessage());
        }
    }

    @Override
    public CacheUser findCacheByUserId(Long userId, CacheSpec cacheSpec) {
        String jsonString = redisTemplate.opsForValue().get(cacheSpec.generateKey(userId));
        try {
            return objectMapper.readValue(jsonString, cacheSpec.getClazz());
        } catch (JsonProcessingException e) {
            log.error(e.getMessage());
            throw new CustomException(ExceptionCode.CONVERSION_EXCEPTION);
        }
    }
}
