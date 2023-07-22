package com.tagstory.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "users")
public class User extends BaseTime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;

    @Column(nullable = false)
    private String userKey;

    @Column(nullable = false)
    private String email;

    private String nickname;

    private String refreshToken;

    @Enumerated(EnumType.STRING)
    private Role role;

    @Enumerated(EnumType.STRING)
    private UserStatus userStatus;


    /*
     * 비즈니스 로직
     */
    public static User register(String userKey, String email, String refreshToken) {
        return User.builder()
                .userKey(userKey)
                .email(email)
                .role(Role.ROLE_USER)
                .userStatus(UserStatus.ACTIVE)
                .refreshToken(refreshToken)
                .build();
    }

}