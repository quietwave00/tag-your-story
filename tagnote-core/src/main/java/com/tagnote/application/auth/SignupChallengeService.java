package com.tagnote.application.auth;

import com.tagnote.application.auth.port.SignupChallengeStore;
import com.tagnote.application.auth.port.SignupMailSender;
import com.tagnote.core.domain.user.EmailNormalizer;
import com.tagnote.core.exception.CustomException;
import com.tagnote.core.exception.ExceptionCode;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SignupChallengeService {
    private final SignupChallengeStore store;
    private final SignupMailSender sender;
    private final SignupCodeVerifier signer;
    private final SecureRandom random = new SecureRandom();

    // passwordHash must be a password encoder output. The raw password never crosses this boundary.
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public String start(String email, String passwordHash, String clientIp) {
        requireConfigured();
        String normalized = EmailNormalizer.normalize(email);
        validate(passwordHash, clientIp);
        String id = UUID.randomUUID().toString();
        String code = code();
        String verifier = signer.sign("code", id + ":" + code);
        int result = storage(() -> store.reserveNew(id, normalized, passwordHash, verifier,
                signer.sign("email-rate", normalized), signer.sign("ip-rate", clientIp)));
        if (result != 1) throw new CustomException(ExceptionCode.SIGNUP_RATE_LIMITED);
        deliver(id, normalized, code, verifier);
        return id;
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void resend(String id, String clientIp) {
        requireConfigured();
        validateId(id);
        if (clientIp == null || clientIp.isBlank()) throw new IllegalArgumentException("Client IP is required");
        // The store owns the normalized email. Reading it before reservation does not authorize
        // a send; reserveResend atomically checks the current challenge and limits again.
        SignupChallengeStore.Verification snapshot = storage(() -> store.verify(id, null));
        if (snapshot.status() != SignupChallengeStore.Verification.Status.VALID) {
            throw new CustomException(ExceptionCode.SIGNUP_CHALLENGE_INVALID);
        }
        String code = code();
        String verifier = signer.sign("code", id + ":" + code);
        int result = storage(() -> store.reserveResend(id, verifier,
                signer.sign("email-rate", snapshot.email()), signer.sign("ip-rate", clientIp)));
        if (result == 0) throw new CustomException(ExceptionCode.SIGNUP_CHALLENGE_INVALID);
        if (result != 1) throw new CustomException(ExceptionCode.SIGNUP_RATE_LIMITED);
        deliver(id, snapshot.email(), code, verifier);
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public SignupChallengeStore.Verification verify(String id, String code) {
        validateId(id);
        if (code == null || !code.matches("[0-9]{6}")) {
            throw new CustomException(ExceptionCode.SIGNUP_CODE_INVALID);
        }
        requireConfigured();
        return storage(() -> store.verify(id, signer.sign("code", id + ":" + code)));
    }

    private void deliver(String id, String email, String code, String verifier) {
        try {
            sender.sendCode(email, code);
        } catch (RuntimeException failure) {
            discardBestEffort(id, verifier);
            throw new CustomException(ExceptionCode.SIGNUP_MAIL_UNAVAILABLE);
        }
        try {
            if (!store.activate(id, verifier)) {
                throw new CustomException(ExceptionCode.SIGNUP_CHALLENGE_UNAVAILABLE);
            }
        } catch (RuntimeException failure) {
            discardBestEffort(id, verifier);
            throw new CustomException(ExceptionCode.SIGNUP_CHALLENGE_UNAVAILABLE);
        }
    }

    private void discardBestEffort(String id, String verifier) {
        try { store.discard(id, verifier); } catch (RuntimeException ignored) {
            // Reservation expires in 30 seconds even when Redis is unavailable for compensation.
        }
    }

    private void requireConfigured() {
        if (!signer.configured()) throw new CustomException(ExceptionCode.SIGNUP_MAIL_UNAVAILABLE);
    }

    private void validate(String hash, String ip) {
        if (hash == null || hash.isBlank() || ip == null || ip.isBlank()) {
            throw new IllegalArgumentException("Password hash and client IP are required");
        }
    }

    private void validateId(String id) {
        try { UUID.fromString(id); } catch (RuntimeException e) {
            throw new CustomException(ExceptionCode.SIGNUP_CHALLENGE_INVALID);
        }
    }

    private String code() {
        return String.format(Locale.ROOT, "%06d", random.nextInt(1_000_000));
    }

    private <T> T storage(java.util.function.Supplier<T> operation) {
        try { return operation.get(); } catch (DataAccessException e) {
            throw new CustomException(ExceptionCode.SIGNUP_CHALLENGE_UNAVAILABLE);
        }
    }
}
