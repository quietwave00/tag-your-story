package com.tagstory.core.domain.user.repository;

import com.tagstory.core.config.CacheSpec;
import com.tagstory.core.domain.user.UserEntity;
import com.tagstory.core.domain.user.repository.dto.CacheUser;

public interface CacheUserRepository {

     void saveCache(CacheUser cacheUser, CacheSpec cacheSpec);

    UserEntity findCacheByUserId(Long userId, CacheSpec cacheSpec);
}
