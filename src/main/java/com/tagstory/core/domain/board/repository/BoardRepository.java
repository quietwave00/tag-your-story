package com.tagstory.core.domain.board.repository;

import com.tagstory.core.domain.board.BoardEntity;
import com.tagstory.core.domain.board.BoardStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BoardRepository extends JpaRepository<BoardEntity, Long> {
    List<BoardEntity> findByStatusAndTrackIdOrderByBoardIdDesc(BoardStatus post, String trackId);
}
