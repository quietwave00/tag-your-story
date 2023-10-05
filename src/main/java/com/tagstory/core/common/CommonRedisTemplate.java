package com.tagstory.core.common;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.oauth2.sdk.util.StringUtils;
import com.tagstory.api.exception.CustomException;
import com.tagstory.api.exception.ExceptionCode;
import com.tagstory.core.config.CacheSpec;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
@Slf4j
public class CommonRedisTemplate {
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    /*
     * 데이터를 레디스에 저장한다.
     */
    public <V> void set(Object id, V value, CacheSpec cacheSpec) {
        try {
            String jsonString =  objectMapper.writeValueAsString(value);
            redisTemplate.opsForValue().set(cacheSpec.generateKey(id), jsonString, cacheSpec.getTtl());
        } catch (JsonProcessingException e) {
            throw new CustomException(ExceptionCode.CONVERSION_EXCEPTION);
        }
    }

    /*
     * 데이터를 레디스에서 가져온다.
     */
    public <T> T get(Object id, CacheSpec cacheSpec) {
        String json = redisTemplate.opsForValue().get(cacheSpec.generateKey(id));
        if (StringUtils.isNotBlank(json)) {
            try {
                return objectMapper.readValue(json, cacheSpec.getClazz());
            } catch (JsonProcessingException e) {
                throw new CustomException(ExceptionCode.CONVERSION_EXCEPTION);
            }
        }
        return null;
    }

    /*
     * 데이터를 레디스에서 지운다.
     */
    public boolean delete(Object id, CacheSpec cacheSpec) {
        try {
            String key = cacheSpec.generateKey(id);
            return redisTemplate.delete(key);
        } catch (NullPointerException e) {
            throw new RuntimeException("An exception occurred while deleting the cache.");
        }
    }
}
