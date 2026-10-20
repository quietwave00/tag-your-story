package com.tagnote.application.user;

import com.tagnote.core.domain.user.Role;
import com.tagnote.core.domain.user.UserEntity;
import com.tagnote.core.domain.user.UserStatus;
import com.tagnote.core.domain.user.repository.UserRepository;
import com.tagnote.core.exception.CustomException;
import com.tagnote.core.exception.ExceptionCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AccountWithdrawalWriter {
    private final UserRepository users;

    @Transactional
    public String withdraw(Long userId) {
        UserEntity user = users.findByUserIdForUpdate(userId)
                .orElseThrow(() -> new CustomException(ExceptionCode.USER_NOT_FOUND));
        if (user.getUserStatus() == UserStatus.WITHDRAWN) {
            throw new CustomException(ExceptionCode.ACCOUNT_WITHDRAWN);
        }
        if (user.getUserStatus() != UserStatus.ACTIVE || user.getRole() != Role.ROLE_USER) {
            throw new CustomException(ExceptionCode.USER_INACTIVE);
        }
        String oldUserKey = user.getUserKey();
        user.withdraw();
        users.flush();
        return oldUserKey;
    }
}
