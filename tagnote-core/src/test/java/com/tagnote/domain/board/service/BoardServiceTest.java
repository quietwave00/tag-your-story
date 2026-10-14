package com.tagnote.domain.board.service;

import com.tagnote.core.domain.board.BoardEntity;
import com.tagnote.core.domain.board.BoardStatus;
import com.tagnote.core.domain.board.service.BoardService;
import com.tagnote.infrastructure.persistence.board.BoardJpaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BoardServiceTest {

    @Mock
    private BoardJpaRepository boardRepository;

    @InjectMocks
    private BoardService boardService;

    @Test
    void legacy_reference_조회는_POST_Board만_조회한다() {
        BoardEntity board = BoardEntity.builder().boardId("board-1").status(BoardStatus.POST).build();
        when(boardRepository.findByBoardIdAndStatus("board-1", BoardStatus.POST))
                .thenReturn(Optional.of(board));

        assertThat(boardService.getBoardEntityByBoardId("board-1")).isSameAs(board);
    }

    @Test
    void like_count는_atomic_update를_위임한다() {
        boardService.increaseLikeCount("board-1");
        boardService.decreaseLikeCount("board-1");

        verify(boardRepository).updateLikeCount("board-1", 1);
        verify(boardRepository).updateLikeCount("board-1", -1);
    }
}
