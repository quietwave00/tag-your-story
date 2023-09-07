package com.tagstory.core.domain.user.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tagstory.core.config.CacheSpec;
import com.tagstory.core.domain.user.UserEntity;
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

    public void saveCache(UserEntity userEntity, CacheSpec cacheSpec) {
        try {
            String jsonString = objectMapper.writeValueAsString(userEntity);
            redisTemplate.opsForValue().set(cacheSpec.generateKey(userEntity.getUserId()), jsonString);
        } catch (JsonProcessingException e) {
            log.error(e.getMessage());
        }
    }

    public UserEntity findCacheByUserId(Long userId, CacheSpec cacheSpec) {
        String jsonString = redisTemplate.opsForValue().get(cacheSpec.generateKey(userId));
        try {
            return objectMapper.readValue(jsonString, cacheSpec.getClazz());
        } catch (JsonProcessingException e) {
            log.error(e.getMessage());
        }
        return null;
    }
}
