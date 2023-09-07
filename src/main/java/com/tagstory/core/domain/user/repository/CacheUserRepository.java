package com.tagstory.core.domain.user.repository;

import com.tagstory.core.config.CacheSpec;
import com.tagstory.core.domain.user.UserEntity;

public interface CacheUserRepository {

     void saveCache(UserEntity userEntity, CacheSpec cacheSpec);

    UserEntity findCacheByUserId(Long userId, CacheSpec cacheSpec);
}
