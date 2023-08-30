package com.tagstory.core.domain.user;

import com.tagstory.core.domain.BaseTime;
import com.tagstory.core.domain.board.Board;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

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

    @OneToMany(mappedBy = "user")
    private List<Board> boardList = new ArrayList<>();

    /*
     * 연관 관계 설정
     */
    public void addBoard(Board board) {
        this.boardList.add(board);
        board.addUser(this);
    }

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