package com.tagstory.core.domain.user.repository.dto;

import com.tagstory.core.domain.user.Role;
import com.tagstory.core.domain.user.UserEntity;
import com.tagstory.core.domain.user.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CacheUser {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;

    @Column(nullable = false)
    private String userKey;

    @Column(nullable = false)
    private String email;

    private String nickname;

    @Enumerated(EnumType.STRING)
    private Role role;

    @Enumerated(EnumType.STRING)
    private UserStatus userStatus;

    public static CacheUser create(UserEntity user) {
        return CacheUser.builder()
                .userId(user.getUserId())
                .userKey(user.getUserKey())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .role(user.getRole())
                .userStatus(user.getUserStatus())
                .build();
    }
}
