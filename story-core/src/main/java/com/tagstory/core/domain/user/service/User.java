package com.tagstory.core.domain.user.service;

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
    /* @Todo:  Response에 유저 정보 노출되어 @JsonIgnore 처리했는데
         메소드 내에서 쓰일 때도 ignore 처리돼서 Response Dto 구조 수정 필요 */

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