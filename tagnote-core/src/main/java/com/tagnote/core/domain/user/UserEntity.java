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
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_users_email", columnNames = "email"),
                @UniqueConstraint(name = "uk_users_user_key", columnNames = "user_key"),
                @UniqueConstraint(name = "uk_users_nickname", columnNames = "nickname"),
                @UniqueConstraint(name = "uk_users_provider_subject", columnNames = {"provider", "subject"})
        },
        indexes = {
                @Index(name = "idx_users_status", columnList = "user_status")
        }
)
public class UserEntity extends BaseTime implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;

    @Column(name = "user_key", nullable = false)
    private String userKey;

    @Column(nullable = false, length = 255)
    private String email;

    @Column(length = 20)
    private String nickname;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private AccountProvider provider;

    @Column(length = 255)
    private String subject;

    @Column(name = "password_hash", length = 255)
    private String passwordHash;

    @Column(columnDefinition = "VARCHAR(255)")
    @Enumerated(EnumType.STRING)
    private Role role;

    @Column(columnDefinition = "VARCHAR(255)")
    @Enumerated(EnumType.STRING)
    private UserStatus userStatus;

    /*
     * 서비스 도메인 형변환
     */
    public User toUser() {
        return User.builder()
                .userId(this.getUserId())
                .userKey(this.getUserKey())
                .email(this.getEmail())
                .provider(this.getProvider())
                .subject(this.getSubject())
                .nickname(this.getNickname())
                .role(this.getRole())
                .userStatus(this.getUserStatus())
                .build();
    }

    public void updateNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getDisplayNickname() {
        return userStatus == UserStatus.WITHDRAWN ? "탈퇴한 사용자" : nickname;
    }

    public void withdraw() {
        if (userId == null || userStatus != UserStatus.ACTIVE) {
            throw new IllegalStateException("Only a persisted active user can withdraw");
        }
        String tombstone = "withdrawn:" + userId;
        userStatus = UserStatus.WITHDRAWN;
        userKey = tombstone;
        subject = provider == AccountProvider.GOOGLE ? tombstone : null;
        email = "withdrawn+" + userId + "@invalid.local";
        passwordHash = null;
        nickname = null;
    }

    @PrePersist
    @PreUpdate
    private void normalizeIdentity() {
        email = EmailNormalizer.normalize(email);
        if (provider == null) {
            // Legacy Google builders and cached pending users have no provider field.
            provider = AccountProvider.GOOGLE;
        }
        if (provider == AccountProvider.GOOGLE) {
            if (subject == null) {
                subject = userKey;
            }
            if (subject == null || subject.isBlank() || !subject.equals(userKey)) {
                throw new IllegalArgumentException("Google subject must equal legacy userKey");
            }
        } else if (subject != null) {
            throw new IllegalArgumentException("Local account must not have a provider subject");
        }
    }
}
