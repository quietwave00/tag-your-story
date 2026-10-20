package com.tagnote.core.domain.user.service;

import com.tagnote.core.domain.user.Role;
import com.tagnote.core.domain.user.AccountProvider;
import com.tagnote.core.domain.user.EmailNormalizer;
import com.tagnote.core.domain.user.UserEntity;
import com.tagnote.core.domain.user.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {
    private Long userId;

    private String userKey;

    private String email;

    private AccountProvider provider;

    private String subject;

    private String nickname;

    private Role role;

    private UserStatus userStatus;

    public String getDisplayNickname() {
        return userStatus == UserStatus.WITHDRAWN ? "탈퇴한 사용자" : nickname;
    }

    /*
     * 형변환
     */
    public UserEntity toEntity() {
        return UserEntity.builder()
                .userId(this.getUserId())
                .userKey(this.getUserKey())
                .email(this.getEmail() == null ? null : EmailNormalizer.normalize(this.getEmail()))
                .provider(this.getProvider() == null ? AccountProvider.GOOGLE : this.getProvider())
                .subject(this.getSubject() == null && this.getProvider() != AccountProvider.LOCAL
                        ? this.getUserKey() : this.getSubject())
                .nickname(this.getNickname())
                .role(this.getRole())
                .userStatus(this.getUserStatus())
                .build();
    }

}
