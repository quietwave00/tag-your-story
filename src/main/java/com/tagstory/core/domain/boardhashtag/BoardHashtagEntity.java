package com.tagstory.core.domain.boardhashtag;

import com.tagstory.core.domain.board.BoardEntity;
import com.tagstory.core.domain.hashtag.HashtagEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Table(name="board_hashtag")
@Entity
public class BoardHashtagEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long boardHashtagId;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "board_id")
    private BoardEntity board;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "hashtag_id")
    private HashtagEntity hashtag;

    /*
     * 연관관계 설정
     */
    public static BoardHashtagEntity of(BoardEntity board, HashtagEntity hashtag) {
        return BoardHashtagEntity.builder()
                .board(board)
                .hashtag(hashtag)
                .build();
    }

    public void addBoard(BoardEntity board) {
        this.board = board;
    }

    public void addHashTag(HashtagEntity hashtag) {
        this.hashtag = hashtag;
        hashtag.addBoardHashTag(this);
    }
}
