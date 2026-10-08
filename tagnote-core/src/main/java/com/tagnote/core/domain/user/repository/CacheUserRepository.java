package com.tagnote.core.domain.user.repository;

import com.tagnote.core.common.CacheSpec;
import com.tagnote.core.domain.user.service.User;

public interface CacheUserRepository {

    User saveCache(User user, CacheSpec cacheSpec);

    void deletePendingUser(User user, CacheSpec cacheSpec);

    User findCachedUserByUserId(Long userId, CacheSpec cacheSpec);

    User findCachedUserByPendingUserId(String pendingUserId, CacheSpec cacheSpec);

    User findCachedUserByUserKey(String userKey, CacheSpec user);
}