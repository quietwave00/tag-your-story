package com.tagnote.core.domain.file.repository;

import com.tagnote.core.common.CacheSpec;
import com.tagnote.core.domain.file.FileEntity;

import java.util.List;

public interface CacheFileRepository {
    void saveCache(List<FileEntity> fileList, CacheSpec cacheSpec);
}
