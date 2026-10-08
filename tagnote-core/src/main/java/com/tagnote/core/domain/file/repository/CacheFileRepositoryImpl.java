package com.tagnote.core.domain.file.repository;

import com.tagnote.core.common.CommonRedisTemplate;
import com.tagnote.core.common.CacheSpec;
import com.tagnote.core.domain.file.FileEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
@Slf4j
public class CacheFileRepositoryImpl implements CacheFileRepository {

    private final CommonRedisTemplate redisTemplate;

    @Override
    public void saveCache(List<FileEntity> fileList, CacheSpec cacheSpec) {
        fileList.forEach(file -> {
            String cacheKey = cacheSpec.generateKey(file.getFileId());
            redisTemplate.set(cacheKey, file, cacheSpec);
        });
    }
}
