package com.tagstory.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.sql.Timestamp;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userNo;

    @Column(nullable = false)
    private String userKey;

    @Column(nullable = false)
    private String email;

    private String nickname;

    @Column(nullable = false)
    @CreationTimestamp
    private Timestamp createdDate;

    @Enumerated(EnumType.STRING)
    private Role role;

    @Enumerated(EnumType.STRING)
    private Status status;


    /*
     * 비즈니스 로직
     */
    public static User register(String userKey, String email) {
        return User.builder()
                .userKey(userKey)
                .email(email)
                .role(Role.ROLE_USER)
                .status(Status.Y)
                .build();
    }
}