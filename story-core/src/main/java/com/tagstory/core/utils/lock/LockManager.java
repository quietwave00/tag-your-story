package com.tagstory.core.utils.lock;

import com.tagstory.core.domain.like.service.Like;
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

    /**
     * 키값으로 락을 건다.
     * @param name
     */
    public void lock(String name) {
        RLock lock = redissonClient.getLock(name);
        try {
            boolean available = lock.tryLock(2, 3, TimeUnit.SECONDS);
            if (!available) {
                throw new RuntimeException("Try Lock Failed");
            }
        } catch(InterruptedException e) {
                log.error(e.getMessage());
        }
    }

    /**
     * 키 값으로 락을 푼다.
     * @param name
     */
    public void unlock(String name) {
        RLock lock = redissonClient.getLock(name);
        boolean isLocked = lock.isLocked();
        if (isLocked) {
            lock.unlock();
        }
    }

    /**
     * 락 획득 여부를 확인한다.
     * @param name
     * @return
     */
    public boolean isLocked(String name) {
        final RLock lock = redissonClient.getLock(name);
        return lock.isLocked();
    }
}
