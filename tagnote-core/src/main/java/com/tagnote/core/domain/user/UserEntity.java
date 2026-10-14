package com.tagnote.core.domain.user;

import com.tagnote.core.domain.BaseTime;
import com.tagnote.core.domain.user.service.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import java.io.Serializable;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
        name = "users",
        indexes = {
                @Index(name = "idx_user_key", columnList = "userKey")
        }
)
public class UserEntity extends BaseTime implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;

    @Column(nullable = false)
    private String userKey;

    @Column(nullable = false)
    private String email;

    private String nickname;

    @Column(columnDefinition = "VARCHAR(255)")
    @Enumerated(EnumType.STRING)
    private Role role;

    @Column(columnDefinition = "VARCHAR(255)")
    @Enumerated(EnumType.STRING)
    private UserStatus userStatus;

    /*
     * 비즈니스 로직
     */
    public static UserEntity register(String userKey, String email) {
        return builder()
                .userKey(userKey)
                .email(email)
                .role(Role.ROLE_USER)
                .userStatus(UserStatus.ACTIVE)
                .build();
    }

    /*
     * 서비스 도메인 형변환
     */
    public User toUser() {
        return User.builder()
                .userId(this.getUserId())
                .userKey(this.getUserKey())
                .email(this.getEmail())
                .nickname(this.getNickname())
                .role(this.getRole())
                .userStatus(this.getUserStatus())
                .build();
    }

    public void updateNickname(String nickname) {
        this.nickname = nickname;
    }
}
