package com.tagnote.application.auth;

import com.tagnote.application.auth.port.LoginAttemptStore;
import com.tagnote.core.domain.user.AccountProvider;
import com.tagnote.core.domain.user.EmailNormalizer;
import com.tagnote.core.domain.user.Role;
import com.tagnote.core.domain.user.UserEntity;
import com.tagnote.core.domain.user.UserStatus;
import com.tagnote.core.domain.user.repository.UserRepository;
import com.tagnote.core.domain.user.service.User;
import com.tagnote.core.exception.CustomException;
import com.tagnote.core.exception.ExceptionCode;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
public class EmailLoginService {
    private final UserRepository users;
    private final LoginAttemptStore attempts;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    // A fixed BCrypt hash keeps unknown LOCAL emails on the password verification path.
    private static final String DUMMY_HASH = "$2a$10$7EqJtq98hPqEX7fNZaFWoOhi74ydGVJ/K3HKtxGuUm4U1rB.RbX/W";

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public User login(String email, String password, String ip) {
        String normalized;
        try {
            normalized = EmailNormalizer.normalize(email);
        } catch (IllegalArgumentException e) {
            throw new CustomException(ExceptionCode.LOGIN_INVALID_CREDENTIALS);
        }
        if (password == null || password.isBlank() || ip == null || ip.isBlank()) {
            throw new CustomException(ExceptionCode.LOGIN_INVALID_CREDENTIALS);
        }
        String emailKey = digest(normalized);
        String ipKey = digest(ip);
        try {
            if (!attempts.allow(emailKey, ipKey)) throw new CustomException(ExceptionCode.LOGIN_RATE_LIMITED);
        } catch (DataAccessException e) {
            throw new CustomException(ExceptionCode.LOGIN_UNAVAILABLE);
        }

        UserEntity account = users.findByEmail(normalized).orElse(null);
        if (account != null && account.getProvider() == AccountProvider.GOOGLE) {
            throw new CustomException(ExceptionCode.GOOGLE_ACCOUNT_EXISTS);
        }
        String hash = account == null ? DUMMY_HASH : account.getPasswordHash();
        boolean valid = encoder.matches(password, hash == null ? DUMMY_HASH : hash);
        if (account == null || account.getProvider() != AccountProvider.LOCAL || hash == null || !valid) {
            try {
                attempts.recordFailure(emailKey);
            } catch (DataAccessException e) {
                throw new CustomException(ExceptionCode.LOGIN_UNAVAILABLE);
            }
            throw new CustomException(ExceptionCode.LOGIN_INVALID_CREDENTIALS);
        }
        if (account.getUserStatus() != UserStatus.ACTIVE || account.getRole() != Role.ROLE_USER) {
            throw new CustomException(ExceptionCode.USER_INACTIVE);
        }
        try {
            attempts.clearFailures(emailKey);
        } catch (DataAccessException e) {
            throw new CustomException(ExceptionCode.LOGIN_UNAVAILABLE);
        }
        return account.toUser();
    }

    private String digest(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
