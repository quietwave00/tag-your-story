package com.tagstory.core.domain.boardhashtag.repository;

import com.tagstory.core.domain.boardhashtag.BoardHashtagEntity;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BoardHashtagRepository extends JpaRepository<BoardHashtagEntity, Long> {

    @Query("""
                SELECT h.hashtag.name
                FROM BoardHashtagEntity h
                WHERE h.board.boardId = :boardId
                ORDER BY :boardId DESC
                """)
    List<String> findHashtagNameByBoardId(@Param("boardId") String boardId);

    void deleteByBoard_BoardId(String boardId);

    @Query("""
            SELECT bh
            FROM BoardHashtagEntity bh
            JOIN FETCH bh.board b
            JOIN FETCH bh.hashtag h
            WHERE b.status = 'POST'
            ORDER BY b.createdAt DESC
        """)
    List<BoardHashtagEntity> findRecentBoardHashtagList(PageRequest pageRequest);
}