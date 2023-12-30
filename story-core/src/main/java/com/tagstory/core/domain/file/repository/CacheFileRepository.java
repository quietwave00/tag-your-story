package com.tagstory.core.domain.file.repository;

import com.tagstory.core.common.CacheSpec;
import com.tagstory.core.domain.file.FileEntity;

import java.util.List;

public interface CacheFileRepository {
    void saveCache(List<FileEntity> fileList, CacheSpec cacheSpec);
}
