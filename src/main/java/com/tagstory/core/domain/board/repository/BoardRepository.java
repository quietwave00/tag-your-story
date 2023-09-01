package com.tagstory.core.domain.board.repository;

import com.tagstory.core.domain.board.Board;
import com.tagstory.core.domain.board.BoardStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BoardRepository extends JpaRepository<Board, Long> {
    List<Board> findByStatusAndTrackIdOrderByBoardIdDesc(BoardStatus post, String trackId);
}
