package com.tagstory.core.domain.board.repository;

import com.tagstory.core.domain.board.BoardEntity;
import com.tagstory.core.domain.board.BoardStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BoardRepository extends JpaRepository<BoardEntity, Long> {
    Page<BoardEntity> findByStatusAndTrackIdOrderByBoardIdDesc(BoardStatus post, String trackId, Pageable pageable);

    List<BoardEntity> findByTrackId(String trackId);

    Optional<BoardEntity> findByBoardIdAndStatus(String boardId, BoardStatus status);

    Optional<BoardEntity> findByBoardId(String boardId);

    int countByTrackId(String trackId);
}
