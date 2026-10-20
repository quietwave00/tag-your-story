package com.tagnote.infrastructure.external.redis;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
class RedisLoginAttemptStoreTest {
    @Container
    static final GenericContainer<?> redisServer = new GenericContainer<>(DockerImageName.parse("redis:7.2-alpine"))
            .withExposedPorts(6379);
    private LettuceConnectionFactory connection;

    @AfterEach
    void close() {
        if (connection != null) connection.destroy();
    }

    private RedisLoginAttemptStore store() {
        connection = new LettuceConnectionFactory(new RedisStandaloneConfiguration(
                redisServer.getHost(), redisServer.getMappedPort(6379)));
        connection.afterPropertiesSet();
        StringRedisTemplate template = new StringRedisTemplate(connection);
        template.afterPropertiesSet();
        return new RedisLoginAttemptStore(template);
    }

    @Test
    void fiveFailuresLockEmailAndSuccessCanClearFailures() {
        RedisLoginAttemptStore store = store();
        String email = UUID.randomUUID().toString();
        String ip = UUID.randomUUID().toString();
        for (int i = 0; i < 5; i++) {
            assertThat(store.allow(email, ip)).isTrue();
            store.recordFailure(email);
        }
        assertThat(store.allow(email, ip)).isFalse();
        store.clearFailures(email);
        assertThat(store.allow(email, ip)).isTrue();
    }

    @Test
    void emailAndIpRequestBudgetsApplyAcrossAccounts() {
        RedisLoginAttemptStore store = store();
        String email = UUID.randomUUID().toString();
        String ip = UUID.randomUUID().toString();
        for (int i = 0; i < 10; i++) assertThat(store.allow(email, ip)).isTrue();
        assertThat(store.allow(email, ip)).isFalse();
        for (int i = 0; i < 20; i++) assertThat(store.allow(UUID.randomUUID().toString(), ip)).isTrue();
        assertThat(store.allow(UUID.randomUUID().toString(), ip)).isFalse();
    }
}
