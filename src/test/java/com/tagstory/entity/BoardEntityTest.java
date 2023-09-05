package com.tagstory.entity;

import com.tagstory.core.domain.board.BoardEntity;
import com.tagstory.core.domain.board.BoardStatus;
import com.tagstory.core.domain.board.dto.command.CreateBoardCommand;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class BoardEntityTest {


    @Test
    void create() {
        CreateBoardCommand createBoardCommand = CreateBoardCommand.builder()
                .content("test")
                .trackId("testId")
                .build();

        BoardEntity boardEntity = BoardEntity.create(createBoardCommand);

        assertThat(boardEntity.getContent()).isEqualTo("test");
        assertThat(boardEntity.getTrackId()).isEqualTo("testId");
        assertThat(boardEntity.getStatus()).isEqualTo(BoardStatus.POST);
        assertThat(boardEntity.getCount()).isEqualTo(0);
    }

}
