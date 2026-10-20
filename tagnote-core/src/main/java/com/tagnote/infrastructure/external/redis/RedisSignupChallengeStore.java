package com.tagnote.infrastructure.external.redis;

import com.tagnote.application.auth.port.SignupChallengeStore;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RedisSignupChallengeStore implements SignupChallengeStore {
    private static final String PREFIX = "signup:challenge:";
    private static final String EMAIL_RATE = "signup:rate:email:";
    private static final String IP_RATE = "signup:rate:ip:";
    private final StringRedisTemplate redis;
    private final DefaultRedisScript<Long> reserveNew = script("signup/reserve-new.lua", Long.class);
    private final DefaultRedisScript<Long> reserveResend = script("signup/reserve-resend.lua", Long.class);
    private final DefaultRedisScript<Long> activate = script("signup/activate.lua", Long.class);
    private final DefaultRedisScript<Long> discard = script("signup/discard.lua", Long.class);
    private final DefaultRedisScript<Long> release = script("signup/release.lua", Long.class);
    private final DefaultRedisScript<Long> complete = script("signup/complete.lua", Long.class);
    private final DefaultRedisScript<Long> availability = script("signup/availability.lua", Long.class);
    @SuppressWarnings("rawtypes")
    private final DefaultRedisScript<List> claim = script("signup/claim.lua", List.class);
    @SuppressWarnings("rawtypes")
    private final DefaultRedisScript<List> verify = script("signup/verify.lua", List.class);

    public RedisSignupChallengeStore(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public int reserveNew(String id, String email, String passwordHash, String verifier, String emailKey, String ipKey) {
        Long result = redis.execute(reserveNew, List.of(PREFIX + id, EMAIL_RATE + emailKey, IP_RATE + ipKey),
                email, passwordHash, verifier);
        return required(result).intValue();
    }

    @Override
    public int reserveResend(String id, String verifier, String emailKey, String ipKey) {
        Long result = redis.execute(reserveResend, List.of(PREFIX + id, EMAIL_RATE + emailKey, IP_RATE + ipKey), verifier);
        return required(result).intValue();
    }

    @Override
    public boolean activate(String id, String verifier) {
        return Long.valueOf(1).equals(required(redis.execute(activate, List.of(PREFIX + id), verifier)));
    }

    @Override
    public void discard(String id, String verifier) {
        redis.execute(discard, List.of(PREFIX + id), verifier);
    }

    @Override
    public Verification verify(String id, String verifier) {
        @SuppressWarnings("unchecked")
        List<String> result = redis.execute(verify, List.of(PREFIX + id), verifier == null ? "" : verifier);
        if (result == null || result.isEmpty()) throw new DataAccessResourceFailureException("Empty signup verification result");
        return switch (result.get(0)) {
            case "VALID" -> new Verification(Verification.Status.VALID, result.get(1), result.get(2));
            case "PENDING" -> new Verification(Verification.Status.PENDING, null, null);
            case "INVALID" -> new Verification(Verification.Status.INVALID, null, null);
            default -> new Verification(Verification.Status.EXPIRED, null, null);
        };
    }

    @Override
    public Claim claim(String id, String verifier, String owner) {
        @SuppressWarnings("unchecked")
        List<String> result = redis.execute(claim, List.of(PREFIX + id), verifier, owner);
        if (result == null || result.isEmpty()) throw new DataAccessResourceFailureException("Empty signup claim result");
        return switch (result.get(0)) {
            case "CLAIMED" -> new Claim(Claim.Status.CLAIMED, result.get(1), result.get(2), null);
            case "COMPLETE" -> new Claim(Claim.Status.COMPLETE, null, null, Long.valueOf(result.get(1)));
            case "INVALID" -> new Claim(Claim.Status.INVALID, null, null, null);
            case "BUSY" -> new Claim(Claim.Status.BUSY, null, null, null);
            default -> new Claim(Claim.Status.EXPIRED, null, null, null);
        };
    }

    @Override
    public void release(String id, String verifier, String owner) {
        required(redis.execute(release, List.of(PREFIX + id), verifier, owner));
    }

    @Override
    public void complete(String id, String verifier, String owner, Long userId) {
        required(redis.execute(complete, List.of(PREFIX + id), verifier, owner, userId.toString()));
    }

    @Override
    public boolean allowAvailability(String emailKey, String ipKey) {
        return Long.valueOf(1).equals(required(redis.execute(availability,
                List.of("signup:availability:email:" + emailKey, "signup:availability:ip:" + ipKey))));
    }

    private static <T> DefaultRedisScript<T> script(String path, Class<T> resultType) {
        DefaultRedisScript<T> script = new DefaultRedisScript<>();
        script.setLocation(new ClassPathResource(path));
        script.setResultType(resultType);
        return script;
    }

    private static Long required(Long value) {
        if (value == null) throw new DataAccessResourceFailureException("Empty signup storage result");
        return value;
    }
}
