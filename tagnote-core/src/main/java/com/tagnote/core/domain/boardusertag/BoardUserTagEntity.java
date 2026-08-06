package com.tagnote.core.domain.boardusertag;

import com.tagnote.core.domain.board.BoardEntity;
import com.tagnote.core.domain.boardusertag.service.BoardUserTag;
import com.tagnote.core.domain.usertag.UserTagEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;

@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Table(name="board_user_tag")
@Entity
public class BoardUserTagEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long boardUserTagId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "board_id")
    private BoardEntity board;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
    @JoinColumn(name = "user_tag_id")
    private UserTagEntity userTag;

    /*
     * 연관관계 설정
     */
    public static BoardUserTagEntity of(BoardEntity board, UserTagEntity userTag) {
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
