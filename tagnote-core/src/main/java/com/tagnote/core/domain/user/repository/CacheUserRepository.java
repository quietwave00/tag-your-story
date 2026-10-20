package com.tagnote.core.domain.user.repository;

import com.tagnote.core.common.CacheSpec;
import com.tagnote.core.domain.user.service.User;

public interface CacheUserRepository {

    User saveCache(User user, CacheSpec cacheSpec);

    User findCachedUserByUserId(Long userId, CacheSpec cacheSpec);

    User findCachedUserByUserKey(String userKey, CacheSpec user);

    void deleteCachedUser(Long userId, String userKey);
}
