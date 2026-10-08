package com.tagnote.core.domain.boardusertag;

import com.tagnote.core.domain.board.BoardEntity;
import com.tagnote.core.domain.BaseTime;
import com.tagnote.core.domain.boardusertag.service.BoardUserTag;
import com.tagnote.core.domain.usertag.UserTagEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;

import java.util.Objects;

@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Table(
        name = "board_user_tag",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_board_user_tag_board_tag",
                columnNames = {"board_id", "user_tag_id"}
        ),
        indexes = @Index(name = "idx_board_user_tag_tag_board", columnList = "user_tag_id,board_id")
)
@Entity
public class BoardUserTagEntity extends BaseTime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long boardUserTagId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "board_id", nullable = false)
    private BoardEntity board;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_tag_id", nullable = false)
    private UserTagEntity userTag;

    /*
     * 연관관계 설정
     */
    public static BoardUserTagEntity of(BoardEntity board, UserTagEntity userTag) {
        if (board.getUserEntity() == null || userTag.getOwner() == null
                || !Objects.equals(board.getUserEntity().getUserId(), userTag.getOwner().getUserId())) {
            throw new IllegalArgumentException("UserTag owner must match Board writer");
        }
        return builder()
                .board(board)
                .userTag(userTag)
                .build();
    }

    public void addBoard(BoardEntity board) {
        this.board = board;
    }


    /*
     * 형변환
     */
    public BoardUserTag toBoardUserTag() {
        return BoardUserTag.builder()
                .boardUserTagId(this.getBoardUserTagId())
                .boardId(this.getBoard().getBoardId())
                .userTag(this.getUserTag().toUserTag())
                .build();
    }
}
