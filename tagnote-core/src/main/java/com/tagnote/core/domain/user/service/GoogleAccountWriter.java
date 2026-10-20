package com.tagnote.core.domain.user.service;

import com.tagnote.core.domain.user.AccountProvider;
import com.tagnote.core.domain.user.Role;
import com.tagnote.core.domain.user.UserEntity;
import com.tagnote.core.domain.user.UserStatus;
import com.tagnote.core.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GoogleAccountWriter {
    private final UserRepository users;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public User create(String subject, String email, String nickname) {
        UserEntity entity = UserEntity.builder()
                .userKey(subject).provider(AccountProvider.GOOGLE).subject(subject)
                .email(email).nickname(nickname)
                .role(Role.ROLE_USER).userStatus(UserStatus.ACTIVE).build();
        return users.saveAndFlush(entity).toUser();
    }
}
