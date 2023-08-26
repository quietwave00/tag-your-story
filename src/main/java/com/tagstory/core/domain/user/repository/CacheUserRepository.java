package com.tagstory.core.domain.user.repository;

import com.tagstory.core.config.CacheSpec;
import com.tagstory.core.domain.user.User;

public interface CacheUserRepository {

     void saveCache(User user, CacheSpec cacheSpec);

    User findCacheByUserId(Long userId, CacheSpec cacheSpec);
}
