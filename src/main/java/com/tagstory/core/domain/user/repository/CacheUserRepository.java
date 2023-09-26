package com.tagstory.core.domain.user.repository;

import com.tagstory.core.config.CacheSpec;
import com.tagstory.core.domain.user.repository.dto.CacheUser;

public interface CacheUserRepository {

    void saveCache(CacheUser cacheUser, CacheSpec cacheSpec);

    CacheUser savePendingUser(CacheUser cacheUser, CacheSpec cacheSpec);

    CacheUser findCacheByUserId(Long userId, CacheSpec cacheSpec);

    CacheUser findPendingUserByTempId(String tempId, CacheSpec cacheSpec);
}
