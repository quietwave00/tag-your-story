package com.tagstory.core.domain.boardhashtag;

import com.tagstory.core.domain.BaseTime;
import com.tagstory.core.domain.board.BoardEntity;
import com.tagstory.core.domain.hashtag.HashtagEntity;
import lombok.Getter;

import javax.persistence.*;

@Getter
@Table(name="BOARD_HASHTAG")
@Entity
public class BoardHashtagEntity extends BaseTime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private BoardHashtagId boardHashtagId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hashtag_id")
    private HashtagEntity hashtag;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "board_id")
    private BoardEntity board;

    /*
     * 연관관계 설정
     */
    public void add(BoardEntity board) {
        this.board = board;
        this.boardHashtagId = new BoardHashtagId(board.getBoardId(), hashtag.getHashtagId());
    }
}
