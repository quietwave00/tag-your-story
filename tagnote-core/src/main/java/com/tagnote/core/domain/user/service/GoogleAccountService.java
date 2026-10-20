package com.tagnote.core.domain.user.service;

import com.tagnote.core.domain.user.AccountProvider;
import com.tagnote.core.domain.user.EmailNormalizer;
import com.tagnote.core.domain.user.Role;
import com.tagnote.core.domain.user.UserEntity;
import com.tagnote.core.domain.user.UserStatus;
import com.tagnote.core.domain.user.repository.UserRepository;
import com.tagnote.core.exception.CustomException;
import com.tagnote.core.exception.ExceptionCode;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GoogleAccountService {
    private static final int MAX_ATTEMPTS = 5;

    private final UserRepository users;
    private final GoogleAccountWriter writer;
    private final GoogleNicknameGenerator nicknames;

    // Called after Google userinfo has returned, with no surrounding DB transaction.
    public User loginOrCreate(String subject, String email) {
        if (subject == null || subject.isBlank()) {
            throw new CustomException(ExceptionCode.GOOGLE_IDENTITY_INVALID);
        }
        String normalizedEmail;
        try {
            normalizedEmail = EmailNormalizer.normalize(email);
        } catch (IllegalArgumentException e) {
            throw new CustomException(ExceptionCode.GOOGLE_EMAIL_UNVERIFIED);
        }

        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            UserEntity existing = users.findByProviderAndSubject(AccountProvider.GOOGLE, subject).orElse(null);
            if (existing != null) {
                return active(existing);
            }
            rejectEmailConflict(normalizedEmail);
            String nickname = nicknames.generate();
            try {
                return writer.create(subject, normalizedEmail, nickname);
            } catch (DataIntegrityViolationException conflict) {
                // The writer transaction has rolled back. Read committed state before retrying.
                existing = users.findByProviderAndSubject(AccountProvider.GOOGLE, subject).orElse(null);
                if (existing != null) {
                    return active(existing);
                }
                rejectEmailConflict(normalizedEmail);
                if (!users.existsByNickname(nickname)) {
                    throw new CustomException(ExceptionCode.GOOGLE_SIGNUP_CONFLICT);
                }
            }
        }
        throw new CustomException(ExceptionCode.GOOGLE_SIGNUP_CONFLICT);
    }

    private void rejectEmailConflict(String email) {
        users.findByEmail(email).ifPresent(existing -> {
            throw new CustomException(existing.getProvider() == AccountProvider.LOCAL
                    ? ExceptionCode.GOOGLE_EMAIL_CONFLICT_LOCAL
                    : ExceptionCode.GOOGLE_EMAIL_CONFLICT_GOOGLE);
        });
    }

    private User active(UserEntity existing) {
        if (existing.getUserStatus() != UserStatus.ACTIVE || existing.getRole() != Role.ROLE_USER) {
            throw new CustomException(ExceptionCode.USER_INACTIVE);
        }
        return existing.toUser();
    }
}
