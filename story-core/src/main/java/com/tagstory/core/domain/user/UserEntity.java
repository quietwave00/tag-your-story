package com.tagstory.core.domain.user;

import com.tagstory.core.domain.BaseTime;
import com.tagstory.core.domain.board.BoardEntity;
import com.tagstory.core.domain.user.service.dto.response.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

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

    @Enumerated(EnumType.STRING)
    private Role role;

    @Enumerated(EnumType.STRING)
    private UserStatus userStatus;

    @Builder.Default
    @OneToMany(mappedBy = "userEntity")
    private List<BoardEntity> boardList = new ArrayList<>();

    /*
     * 연관 관계 설정
     */
    public void addBoard(BoardEntity board) {
        this.boardList.add(board);
    }

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