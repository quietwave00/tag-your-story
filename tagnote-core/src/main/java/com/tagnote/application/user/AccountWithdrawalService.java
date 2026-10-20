package com.tagnote.application.user;

import com.tagnote.core.common.CacheSpec;
import com.tagnote.core.common.CommonRedisTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountWithdrawalService {
    private final AccountWithdrawalWriter writer;
    private final CommonRedisTemplate redis;

    public void withdraw(Long userId) {
        // The writer's transaction commits before external cache operations begin.
        String oldUserKey = writer.withdraw(userId);
        try {
            redis.delete(userId, CacheSpec.REFRESH_TOKEN);
        } catch (RuntimeException e) {
            log.warn("Refresh token cleanup failed for withdrawn user {}", userId, e);
        }
        try {
            redis.delete(userId, CacheSpec.USER);
        } catch (RuntimeException e) {
            log.warn("User ID cache cleanup failed for withdrawn user {}", userId, e);
        }
        try {
            redis.delete(oldUserKey, CacheSpec.USER);
        } catch (RuntimeException e) {
            log.warn("User key cache cleanup failed for withdrawn user {}", userId, e);
        }
    }
}
