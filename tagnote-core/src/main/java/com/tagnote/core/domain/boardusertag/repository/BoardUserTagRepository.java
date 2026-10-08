package com.tagnote.core.domain.boardusertag.repository;

import com.tagnote.core.domain.boardusertag.BoardUserTagEntity;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BoardUserTagRepository extends JpaRepository<BoardUserTagEntity, Long> {

    @Query("""
                SELECT h.userTag.name
                FROM BoardUserTagEntity h
                WHERE h.board.boardId = :boardId
                ORDER BY :boardId DESC
                """)
    List<String> findUserTagNameByBoardId(@Param("boardId") String boardId);

    void deleteByBoard_BoardId(String boardId);

    @Query("""
            SELECT bh
            FROM BoardUserTagEntity bh
            JOIN FETCH bh.board b
            JOIN FETCH bh.userTag h
            WHERE b.status = 'POST'
            ORDER BY b.createdAt DESC
        """)
    List<BoardUserTagEntity> findRecentBoardUserTagList(PageRequest pageRequest);
}