package com.tagstory.entity;

import com.tagstory.core.domain.board.Board;
import com.tagstory.core.domain.board.BoardStatus;
import com.tagstory.core.domain.board.dto.receive.ReceiveCreateBoard;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class BoardTest {


    @Test
    void create() {
        ReceiveCreateBoard receiveCreateBoard = ReceiveCreateBoard.builder()
                .content("test")
                .trackId("testId")
                .build();

        Board board = Board.create(receiveCreateBoard);

        assertThat(board.getContent()).isEqualTo("test");
        assertThat(board.getTrackId()).isEqualTo("testId");
        assertThat(board.getStatus()).isEqualTo(BoardStatus.POST);
        assertThat(board.getCount()).isEqualTo(0);
    }

}
