package com.tagstory.core.utils.lock;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class LockManager {
    private final RedissonClient redissonClient;

    /*
     * 키값으로 락을 건다.
     */
    public void lock(String key) {
        log.info("락 진입");
        RLock lock = redissonClient.getLock(key);
        try {
            boolean available = lock.tryLock(10, 1, TimeUnit.SECONDS);
            if (!available) {
                log.warn("redisson getLock timeout");
                throw new IllegalArgumentException();
            }
        } catch(InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            lock.unlock();
            log.info("락 해제");
        }
    }
}
