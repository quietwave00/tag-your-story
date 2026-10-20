package com.tagnote.application.auth;

import com.tagnote.application.auth.port.SignupChallengeStore;
import com.tagnote.core.domain.user.AccountProvider;
import com.tagnote.core.domain.user.EmailNormalizer;
import com.tagnote.core.domain.user.Role;
import com.tagnote.core.domain.user.UserEntity;
import com.tagnote.core.domain.user.UserStatus;
import com.tagnote.core.domain.user.repository.UserRepository;
import com.tagnote.core.domain.user.service.GoogleNicknameGenerator;
import com.tagnote.core.domain.user.service.User;
import com.tagnote.core.exception.CustomException;
import com.tagnote.core.exception.ExceptionCode;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EmailSignupService {
    private final SignupChallengeService challenges;
    private final SignupChallengeStore store;
    private final SignupCodeVerifier signer;
    private final UserRepository users;
    private final LocalAccountWriter writer;
    private final GoogleNicknameGenerator nicknames;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public String start(String email, String password, String confirmation, String ip) {
        String normalized = normalize(email);
        validatePassword(password, confirmation);
        checkAvailability(normalized, ip);
        return challenges.start(normalized, encoder.encode(password), ip);
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void resend(String challengeId, String ip) {
        challenges.resend(challengeId, ip);
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public boolean available(String email, String ip) {
        String normalized = normalize(email);
        checkAvailability(normalized, ip);
        return true;
    }

    private void checkAvailability(String normalized, String ip) {
        if (ip == null || ip.isBlank()) throw new CustomException(ExceptionCode.SIGNUP_INPUT_INVALID);
        if (!signer.configured()) throw new CustomException(ExceptionCode.SIGNUP_CHALLENGE_UNAVAILABLE);
        if (!storage(() -> store.allowAvailability(signer.sign("availability-email", normalized),
                signer.sign("availability-ip", ip)))) {
            throw new CustomException(ExceptionCode.SIGNUP_RATE_LIMITED);
        }
        rejectExisting(normalized);
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public User confirm(String challengeId, String code) {
        if (challengeId == null || !isUuid(challengeId)) throw new CustomException(ExceptionCode.SIGNUP_CHALLENGE_INVALID);
        if (code == null || !code.matches("[0-9]{6}")) throw new CustomException(ExceptionCode.SIGNUP_CODE_INVALID);
        if (!signer.configured()) throw new CustomException(ExceptionCode.SIGNUP_CHALLENGE_UNAVAILABLE);
        String verifier = signer.sign("code", challengeId + ":" + code);
        String userKey = "local:" + signer.sign("local-user-key", challengeId);
        String owner = UUID.randomUUID().toString();
        SignupChallengeStore.Claim claim = storage(() -> store.claim(challengeId, verifier, owner));
        if (claim.status() == SignupChallengeStore.Claim.Status.COMPLETE) {
            return users.findById(claim.userId()).filter(u -> u.getProvider() == AccountProvider.LOCAL)
                    .map(this::active).orElseThrow(() -> new CustomException(ExceptionCode.SIGNUP_CHALLENGE_INVALID));
        }
        if (claim.status() == SignupChallengeStore.Claim.Status.INVALID) throw new CustomException(ExceptionCode.SIGNUP_CODE_INVALID);
        if (claim.status() == SignupChallengeStore.Claim.Status.BUSY) throw new CustomException(ExceptionCode.SIGNUP_CONFIRM_BUSY);
        if (claim.status() != SignupChallengeStore.Claim.Status.CLAIMED) throw new CustomException(ExceptionCode.SIGNUP_CHALLENGE_INVALID);

        try {
            // The deterministic internal key recovers a committed account if Redis completion failed.
            User existing = users.findByUserKey(userKey).map(this::active).orElse(null);
            if (existing != null) return complete(challengeId, verifier, owner, existing);
            rejectExisting(claim.email());
            for (int attempt = 0; attempt < 5; attempt++) {
                String nickname = nicknames.generate();
                try {
                    return complete(challengeId, verifier, owner,
                            writer.create(userKey, claim.email(), claim.passwordHash(), nickname));
                } catch (DataIntegrityViolationException conflict) {
                    existing = users.findByUserKey(userKey).map(this::active).orElse(null);
                    if (existing != null) return complete(challengeId, verifier, owner, existing);
                    rejectExisting(claim.email());
                    if (!users.existsByNickname(nickname)) throw new CustomException(ExceptionCode.SIGNUP_CONFLICT);
                }
            }
            throw new CustomException(ExceptionCode.SIGNUP_CONFLICT);
        } catch (RuntimeException failure) {
            try { store.release(challengeId, verifier, owner); } catch (DataAccessException ignored) {
                // The claim lease expires after 30 seconds; DB success is recoverable by userKey.
            }
            throw failure;
        }
    }

    private User complete(String id, String verifier, String owner, User user) {
        // The DB transaction has committed before this call. A failed marker is recoverable.
        try { store.complete(id, verifier, owner, user.getUserId()); } catch (DataAccessException ignored) { }
        return user;
    }

    private User active(UserEntity entity) {
        if (entity.getUserStatus() != UserStatus.ACTIVE || entity.getRole() != Role.ROLE_USER) {
            throw new CustomException(ExceptionCode.USER_INACTIVE);
        }
        return entity.toUser();
    }

    private void rejectExisting(String email) {
        users.findByEmail(email).ifPresent(existing -> {
            throw new CustomException(existing.getProvider() == AccountProvider.GOOGLE
                    ? ExceptionCode.GOOGLE_ACCOUNT_EXISTS : ExceptionCode.LOCAL_ACCOUNT_EXISTS);
        });
    }

    private String normalize(String email) {
        try { return EmailNormalizer.normalize(email); } catch (IllegalArgumentException e) {
            throw new CustomException(ExceptionCode.SIGNUP_INPUT_INVALID);
        }
    }

    private void validatePassword(String password, String confirmation) {
        if (password == null || confirmation == null || !password.equals(confirmation)
                || password.length() < 8 || password.getBytes(StandardCharsets.UTF_8).length > 72
                || !password.matches("(?s).*[A-Za-z].*") || !password.matches("(?s).*\\d.*")) {
            throw new CustomException(ExceptionCode.SIGNUP_INPUT_INVALID);
        }
    }

    private boolean isUuid(String id) {
        try { return UUID.fromString(id).toString().equals(id); } catch (IllegalArgumentException e) { return false; }
    }

    private <T> T storage(java.util.function.Supplier<T> operation) {
        try { return operation.get(); } catch (DataAccessException e) {
            throw new CustomException(ExceptionCode.SIGNUP_CHALLENGE_UNAVAILABLE);
        }
    }
}
