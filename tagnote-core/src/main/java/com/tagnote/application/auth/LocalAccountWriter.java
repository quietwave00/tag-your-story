package com.tagnote.application.auth;

import com.tagnote.core.domain.user.AccountProvider;
import com.tagnote.core.domain.user.Role;
import com.tagnote.core.domain.user.UserEntity;
import com.tagnote.core.domain.user.UserStatus;
import com.tagnote.core.domain.user.repository.UserRepository;
import com.tagnote.core.domain.user.service.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LocalAccountWriter {
    private final UserRepository users;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public User create(String userKey, String email, String passwordHash, String nickname) {
        UserEntity entity = UserEntity.builder()
                .userKey(userKey).provider(AccountProvider.LOCAL)
                .email(email).passwordHash(passwordHash).nickname(nickname)
                .role(Role.ROLE_USER).userStatus(UserStatus.ACTIVE).build();
        return users.saveAndFlush(entity).toUser();
    }
}
