package com.tagstory.core.domain.like;

import com.tagstory.core.domain.board.BoardEntity;
import com.tagstory.core.domain.user.UserEntity;
import com.tagstory.core.domain.user.service.dto.response.User;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Getter
@Entity
@Table(name = "likes")
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor
public class LikeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long likeId;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private UserEntity user;

    @ManyToOne
    @JoinColumn(name = "board_id")
    private BoardEntity board;

    /*
     * 생성자
     */
    private LikeEntity(UserEntity user, BoardEntity board) {
        this.user = user;
        this.board = board;
    }

    /*
     * 비즈니스 로직
     */
    public static LikeEntity createLike(BoardEntity board, User user) {
        return new LikeEntity(user.toEntity(), board);
    }
}
