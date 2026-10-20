package com.tagnote.infrastructure.external.redis;

import com.tagnote.application.auth.port.LoginAttemptStore;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RedisLoginAttemptStore implements LoginAttemptStore {
    private static final String PREFIX = "login:limit:";
    private final StringRedisTemplate redis;
    private final DefaultRedisScript<Long> allow = script("login/allow.lua");
    private final DefaultRedisScript<Long> fail = script("login/failure.lua");

    public RedisLoginAttemptStore(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public boolean allow(String emailKey, String ipKey) {
        Long result = redis.execute(allow, List.of(PREFIX + "email:" + emailKey,
                PREFIX + "ip:" + ipKey, PREFIX + "fail:" + emailKey));
        if (result == null) throw new DataAccessResourceFailureException("Empty login limit result");
        return result == 1;
    }

    @Override
    public void recordFailure(String emailKey) {
        if (redis.execute(fail, List.of(PREFIX + "fail:" + emailKey)) == null) {
            throw new DataAccessResourceFailureException("Empty login failure result");
        }
    }

    @Override
    public void clearFailures(String emailKey) {
        redis.delete(PREFIX + "fail:" + emailKey);
    }

    private static DefaultRedisScript<Long> script(String path) {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setLocation(new ClassPathResource(path));
        script.setResultType(Long.class);
        return script;
    }
}
