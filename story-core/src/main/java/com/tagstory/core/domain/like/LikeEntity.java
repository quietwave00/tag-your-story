package com.tagstory.core.domain.like;

import com.tagstory.core.domain.board.BoardEntity;
import com.tagstory.core.domain.board.service.Board;
import com.tagstory.core.domain.like.service.Like;
import com.tagstory.core.domain.user.UserEntity;
import com.tagstory.core.domain.user.service.User;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Builder
@Entity
@Table(name = "likes")
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor
public class LikeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long likeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY)
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
     * 형변환
     */
    public Like toLike() {
        return Like.builder()
                .likeId(this.likeId)
                .board(this.board.toBoard())
                .user(this.user.toUser())
                .build();
    }

    /*
     * 비즈니스 로직
     */
    public static LikeEntity createLike(User user, Board board) {
        return new LikeEntity(user.toEntity(), board.toEntity());
    }
}
