package com.tagnote.core.domain.like;

import com.tagnote.core.domain.board.BoardEntity;
import com.tagnote.core.domain.user.UserEntity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Getter
@Builder
@Entity
@Table(name = "board_like", uniqueConstraints =
        @UniqueConstraint(name = "uk_board_like_user_board", columnNames = {"user_id", "board_id"}), indexes = {
        @Index(name = "idx_board_like_user_created", columnList = "user_id,created_at"),
        @Index(name = "idx_board_like_board_created", columnList = "board_id,created_at")
})
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor
public class LikeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long likeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "board_id", nullable = false)
    private BoardEntity board;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /*
     * 생성자
     */
    private LikeEntity(UserEntity user, BoardEntity board) {
        this.user = user;
        this.board = board;
    }

    @PrePersist
    private void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public static LikeEntity createLike(UserEntity user, BoardEntity board) {
        return new LikeEntity(user, board);
    }
}
