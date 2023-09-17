package com.tagstory.core.domain.user.repository.dto;

import com.tagstory.core.domain.user.Role;
import com.tagstory.core.domain.user.UserEntity;
import com.tagstory.core.domain.user.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CacheUser {
    private Long userId;

    private String userKey;

    private String email;

    private String nickname;

    private Role role;

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

    public static UserEntity toEntity(CacheUser user) {
        return UserEntity.builder()
                .userId(user.getUserId())
                .userKey(user.getUserKey())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .role(user.getRole())
                .userStatus(user.getUserStatus())
                .build();
    }
}
