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
public class NicknameChangeWriter {
    private final UserRepository users;

    @Transactional
    public UserEntity change(Long userId, String nickname) {
        UserEntity user = users.findByUserIdForUpdate(userId)
                .orElseThrow(() -> new CustomException(ExceptionCode.USER_NOT_FOUND));
        if (user.getUserStatus() != UserStatus.ACTIVE || user.getRole() != Role.ROLE_USER) {
            throw new CustomException(ExceptionCode.USER_INACTIVE);
        }
        if (!nickname.equals(user.getNickname())) {
            user.updateNickname(nickname);
            users.flush();
        }
        return user;
    }
}
