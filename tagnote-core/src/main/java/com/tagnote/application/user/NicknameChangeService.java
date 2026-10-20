package com.tagnote.application.user;

import com.tagnote.core.domain.user.UserEntity;
import com.tagnote.core.domain.user.repository.UserRepository;
import com.tagnote.core.exception.CustomException;
import com.tagnote.core.exception.ExceptionCode;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NicknameChangeService {
    private final NicknameChangeWriter writer;
    private final UserRepository users;

    public String change(Long userId, String nickname) {
        if (nickname == null || nickname.isEmpty() || nickname.length() > 20
                || nickname.codePoints().anyMatch(codePoint -> Character.isWhitespace(codePoint)
                        || Character.isSpaceChar(codePoint))) {
            throw new CustomException(ExceptionCode.NICKNAME_INVALID);
        }
        UserEntity user;
        try {
            user = writer.change(userId, nickname);
        } catch (DataIntegrityViolationException conflict) {
            if (users.existsByNickname(nickname)) {
                throw new CustomException(ExceptionCode.NICKNAME_ALREADY_EXISTS);
            }
            throw conflict;
        }
        users.deleteCachedUser(user.getUserId(), user.getUserKey());
        return user.getNickname();
    }
}
