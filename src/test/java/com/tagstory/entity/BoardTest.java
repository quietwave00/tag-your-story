package com.tagstory.entity;

import com.tagstory.core.domain.board.Board;
import com.tagstory.core.domain.board.BoardStatus;
import com.tagstory.core.domain.board.dto.command.CreateBoardCommand;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class BoardTest {


    @Test
    void create() {
        CreateBoardCommand createBoardCommand = CreateBoardCommand.builder()
                .content("test")
                .trackId("testId")
                .build();

        Board board = Board.create(createBoardCommand);

        assertThat(board.getContent()).isEqualTo("test");
        assertThat(board.getTrackId()).isEqualTo("testId");
        assertThat(board.getStatus()).isEqualTo(BoardStatus.POST);
        assertThat(board.getCount()).isEqualTo(0);
    }

}
