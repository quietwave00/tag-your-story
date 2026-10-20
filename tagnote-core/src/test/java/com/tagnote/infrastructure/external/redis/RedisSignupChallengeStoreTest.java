package com.tagnote.infrastructure.external.redis;

import com.tagnote.application.auth.port.SignupChallengeStore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
class RedisSignupChallengeStoreTest {
    @Container
    static final GenericContainer<?> redisServer = new GenericContainer<>(DockerImageName.parse("redis:7.2-alpine"))
            .withExposedPorts(6379);

    private LettuceConnectionFactory connection;

    private RedisSignupChallengeStore store() {
        connection = new LettuceConnectionFactory(new RedisStandaloneConfiguration(
                redisServer.getHost(), redisServer.getMappedPort(6379)));
        connection.afterPropertiesSet();
        StringRedisTemplate template = new StringRedisTemplate(connection);
        template.afterPropertiesSet();
        return new RedisSignupChallengeStore(template);
    }

    @AfterEach
    void close() {
        if (connection != null) connection.destroy();
    }

    @Test
    void codeExpiresAndFiveWrongAttemptsDeleteChallenge() {
        RedisSignupChallengeStore store = store();
        String id = UUID.randomUUID().toString();
        assertThat(store.reserveNew(id, "user@example.com", "$2a$hash", "hmac", "e1", "ip1")).isEqualTo(1);
        assertThat(store.verify(id, "hmac").status()).isEqualTo(SignupChallengeStore.Verification.Status.PENDING);
        assertThat(store.activate(id, "hmac")).isTrue();
        assertThat(connection.getConnection().keyCommands().ttl(("signup:challenge:" + id).getBytes()))
                .isBetween(580L, 600L);
        for (int i = 0; i < 5; i++) {
            assertThat(store.verify(id, "wrong").status()).isEqualTo(SignupChallengeStore.Verification.Status.INVALID);
        }
        assertThat(store.verify(id, "hmac").status()).isEqualTo(SignupChallengeStore.Verification.Status.EXPIRED);
    }

    @Test
    void resendReplacesCodeAndHonorsCooldownAndSendLimit() {
        RedisSignupChallengeStore store = store();
        String id = UUID.randomUUID().toString();
        assertThat(store.reserveNew(id, "user@example.com", "hash", "old", "e2", "ip2")).isEqualTo(1);
        store.activate(id, "old");
        assertThat(store.reserveResend(id, "new", "e2", "ip2")).isEqualTo(2);
        // Move the last-send timestamp back instead of slowing the test for a minute.
        StringRedisTemplate template = new StringRedisTemplate(connection);
        template.afterPropertiesSet();
        template.opsForHash().put("signup:challenge:" + id, "last_send", "1");
        assertThat(store.reserveResend(id, "new", "e2", "ip2")).isEqualTo(1);
        assertThat(store.verify(id, "old").status()).isEqualTo(SignupChallengeStore.Verification.Status.PENDING);
        assertThat(store.activate(id, "new")).isTrue();
        assertThat(store.verify(id, "old").status()).isEqualTo(SignupChallengeStore.Verification.Status.INVALID);
        assertThat(store.verify(id, "new").status()).isEqualTo(SignupChallengeStore.Verification.Status.VALID);
        for (int i = 3; i <= 5; i++) {
            template.opsForHash().put("signup:challenge:" + id, "last_send", "1");
            String next = "code" + i;
            assertThat(store.reserveResend(id, next, "e2", "ip2")).isEqualTo(1);
            store.activate(id, next);
        }
        template.opsForHash().put("signup:challenge:" + id, "last_send", "1");
        assertThat(store.reserveResend(id, "sixth", "e2", "ip2")).isEqualTo(2);
    }

    @Test
    void concurrentStartsWithSameEmailCannotExceedRateLimit() throws Exception {
        RedisSignupChallengeStore store = store();
        var executor = Executors.newFixedThreadPool(10);
        try {
            List<java.util.concurrent.Future<Integer>> results = new ArrayList<>();
            for (int i = 0; i < 10; i++) {
                results.add(executor.submit(() -> store.reserveNew(UUID.randomUUID().toString(),
                        "user@example.com", "hash", "verifier", "e3", "ip3")));
            }
            int accepted = 0;
            for (var result : results) if (result.get(10, TimeUnit.SECONDS) == 1) accepted++;
            assertThat(accepted).isEqualTo(5);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void concurrentResendsOnlyOneReservationCanSend() throws Exception {
        RedisSignupChallengeStore store = store();
        String id = UUID.randomUUID().toString();
        store.reserveNew(id, "user@example.com", "hash", "initial", "e4", "ip4");
        store.activate(id, "initial");
        StringRedisTemplate template = new StringRedisTemplate(connection);
        template.afterPropertiesSet();
        template.opsForHash().put("signup:challenge:" + id, "last_send", "1");

        var executor = Executors.newFixedThreadPool(8);
        try {
            List<java.util.concurrent.Future<Integer>> results = new ArrayList<>();
            for (int i = 0; i < 8; i++) {
                String next = "next" + i;
                results.add(executor.submit(() -> store.reserveResend(id, next, "e4", "ip4")));
            }
            int accepted = 0;
            for (var result : results) if (result.get(10, TimeUnit.SECONDS) == 1) accepted++;
            assertThat(accepted).isEqualTo(1);
            assertThat(store.verify(id, null).status()).isEqualTo(SignupChallengeStore.Verification.Status.PENDING);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void discardInvalidatesFailedDeliveryAndIpLimitIsSharedAcrossEmails() {
        RedisSignupChallengeStore store = store();
        String failed = UUID.randomUUID().toString();
        assertThat(store.reserveNew(failed, "failed@example.com", "hash", "verifier", "e5", "shared-ip"))
                .isEqualTo(1);
        store.discard(failed, "different-verifier");
        assertThat(store.verify(failed, null).status()).isEqualTo(SignupChallengeStore.Verification.Status.PENDING);
        store.discard(failed, "verifier");
        assertThat(store.verify(failed, null).status()).isEqualTo(SignupChallengeStore.Verification.Status.EXPIRED);

        for (int i = 1; i < 20; i++) {
            assertThat(store.reserveNew(UUID.randomUUID().toString(), "user" + i + "@example.com",
                    "hash", "verifier", "unique-email-" + i, "shared-ip")).isEqualTo(1);
        }
        assertThat(store.reserveNew(UUID.randomUUID().toString(), "last@example.com", "hash", "verifier",
                "last-email", "shared-ip")).isEqualTo(2);
    }

    @Test
    void claimIsAtomicAndCompletionCanBeRetried() throws Exception {
        RedisSignupChallengeStore store = store();
        String id = UUID.randomUUID().toString();
        store.reserveNew(id, "user@example.com", "hash", "correct", "claim-email", "claim-ip");
        store.activate(id, "correct");
        var executor = Executors.newFixedThreadPool(8);
        try {
            List<java.util.concurrent.Future<SignupChallengeStore.Claim.Status>> results = new ArrayList<>();
            for (int i = 0; i < 8; i++) {
                String owner = "owner-" + i;
                results.add(executor.submit(() -> store.claim(id, "correct", owner).status()));
            }
            int claimed = 0;
            for (var result : results) if (result.get(10, TimeUnit.SECONDS) == SignupChallengeStore.Claim.Status.CLAIMED) claimed++;
            assertThat(claimed).isEqualTo(1);
            // Identify the owner retained by the atomic claim before marking it complete.
            StringRedisTemplate template = new StringRedisTemplate(connection);
            template.afterPropertiesSet();
            String owner = (String) template.opsForHash().get("signup:challenge:" + id, "owner");
            store.complete(id, "correct", owner, 42L);
            assertThat(store.claim(id, "correct", "repeat").userId()).isEqualTo(42L);
            assertThat(store.claim(id, "wrong", "wrong").status()).isEqualTo(SignupChallengeStore.Claim.Status.INVALID);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void failedClaimCanBeReleasedAndFiveWrongCodesDiscardChallenge() {
        RedisSignupChallengeStore store = store();
        String id = UUID.randomUUID().toString();
        store.reserveNew(id, "user@example.com", "hash", "correct", "failure-email", "failure-ip");
        store.activate(id, "correct");
        assertThat(store.claim(id, "correct", "first").status()).isEqualTo(SignupChallengeStore.Claim.Status.CLAIMED);
        store.release(id, "correct", "other");
        assertThat(store.claim(id, "correct", "second").status()).isEqualTo(SignupChallengeStore.Claim.Status.BUSY);
        store.release(id, "correct", "first");
        for (int i = 0; i < 5; i++) {
            assertThat(store.claim(id, "wrong", "wrong").status()).isEqualTo(SignupChallengeStore.Claim.Status.INVALID);
        }
        assertThat(store.claim(id, "correct", "last").status()).isEqualTo(SignupChallengeStore.Claim.Status.EXPIRED);
    }

    @Test
    void availabilityIsLimitedPerIpAcrossEmails() {
        RedisSignupChallengeStore store = store();
        for (int i = 0; i < 30; i++) assertThat(store.allowAvailability("email-" + i, "same-ip")).isTrue();
        assertThat(store.allowAvailability("another-email", "same-ip")).isFalse();
    }
}
