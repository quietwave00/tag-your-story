package com.tagstory.core.domain.user.repository;

import com.tagstory.core.config.CacheSpec;
import com.tagstory.core.domain.user.service.dto.response.User;

public interface CacheUserRepository {

    User saveCache(User user, CacheSpec cacheSpec);

    boolean deleteCache(User user, CacheSpec cacheSpec);

    User findCachedUserByUserId(Long userId, CacheSpec cacheSpec);

    User findCachedUserByPendingUserId(String pendingUserId, CacheSpec cacheSpec);

    User findCachedUserByUserKey(String userKey, CacheSpec user);
}