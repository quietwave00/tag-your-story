package com.tagstory.core.domain.user.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
public class User {
    private String pendingUserId;

    private Long userId;

    private String userKey;

    private String email;

    private String nickname;

    private Role role;

    private UserStatus userStatus;

    /*
     * 형변환
     */
    public UserEntity toEntity() {
        return UserEntity.builder()
                .userId(this.getUserId())
                .userKey(this.getUserKey())
                .email(this.getEmail())
                .nickname(this.getNickname())
                .role(this.getRole())
                .userStatus(this.getUserStatus())
                .build();
    }

    /*
     * 비즈니스 로직
     */
    public void addNickname(String nickname) {
        this.nickname = nickname;
    }

    public void addRole(Role role) {
        this.role = role;
    }
}