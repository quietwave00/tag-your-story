package com.tagstory.core.domain.user.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tagstory.core.config.CacheSpec;
import com.tagstory.core.domain.user.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
@Slf4j
public class CacheUserRepository {
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public void save(User user, CacheSpec cacheSpec) {
        try {
            String jsonString = objectMapper.writeValueAsString(user);
            redisTemplate.opsForValue().set(cacheSpec.generateKey(user.getUserId()), jsonString);
        } catch (JsonProcessingException e) {
            log.error(e.getMessage());
        }
    }

    public User findByUserId(Long userId, CacheSpec cacheSpec) {
        String jsonString = redisTemplate.opsForValue().get(cacheSpec.generateKey(userId));
        try {
            return objectMapper.readValue(jsonString, cacheSpec.getClazz());
        } catch (JsonProcessingException e) {
            log.error(e.getMessage());
        }
        return null;
    }
}
