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
        log.info("락 진입: {}", key);
        RLock lock = redissonClient.getLock(key);
        boolean available = false;
        try {
            available = lock.tryLock(10, 3, TimeUnit.SECONDS);
            log.info("lock.isLocked(): {}, lock.getName(): {}", lock.isLocked(), lock.getName());
            if (!available) {
                log.warn("redisson getLock timeout");
                throw new IllegalArgumentException();
            }
        } catch(InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 키 값으로 락을 푼다.
     */
    public void unlock(String key) {
        RLock lock = redissonClient.getLock(key);
        boolean available = lock.isLocked();
        if (available) {
            lock.unlock();
        }
        log.info("락 해제");
    }
}
