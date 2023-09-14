package com.tagstory.core.domain.file.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tagstory.core.config.CacheSpec;
import com.tagstory.core.domain.file.FileEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
@Slf4j
public class CacheFileRepositoryImpl implements CacheFileRepository {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public void saveCache(List<FileEntity> fileList, CacheSpec cacheSpec) {
        try {
            String jsonString = objectMapper.writeValueAsString(fileList);
            fileList.forEach(file -> {
                String cacheKey = cacheSpec.generateKey(file.getFileId());
                redisTemplate.opsForValue().set(cacheKey, jsonString);
            });
        } catch(JsonProcessingException e) {
            log.error(e.getMessage());
        }
    }

    @Override
    public List<FileEntity> findCacheByBoardId(Long boardId, CacheSpec cacheSpec) {
//        String jsonString = redisTemplate.opsForValue().get(cacheSpec.generateKey(boardId));
//        try {
//            objectMapper.readValue(jsonString, cacheSpec.getClazz());
//        } catch (JsonProcessingException e) {
//            log.error(e.getMessage());
//            throw new CustomException(ExceptionCode.CONVERSION_EXCEPTION);
//        }
        return null;
    }
}
