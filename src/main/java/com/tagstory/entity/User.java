package com.tagstory.entity;

import lombok.*;

import javax.persistence.*;
import java.io.Serializable;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "users")
public class User extends BaseTime implements Serializable {

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


    /*
     * 비즈니스 로직
     */
    public static User register(String userKey, String email) {
        
        return User.builder()
                .userKey(userKey)
                .email(email)
                .role(Role.ROLE_USER)
                .userStatus(UserStatus.ACTIVE)
                .build();
    }

    public void updateNickname(String nickname) {
        this.nickname = nickname;
    }
}