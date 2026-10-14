package com.tagnote.core.domain.boardusertag.repository;

import com.tagnote.core.domain.boardusertag.BoardUserTagEntity;
import com.tagnote.infrastructure.persistence.board.BoardUserTagNameRow;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Collection;

public interface BoardUserTagRepository extends JpaRepository<BoardUserTagEntity, Long> {

    @Query("""
                SELECT h.userTag.name
                FROM BoardUserTagEntity h
                WHERE h.board.boardId = :boardId
                ORDER BY :boardId DESC
                """)
    List<String> findUserTagNameByBoardId(@Param("boardId") String boardId);

    @Modifying
    @Query("delete from BoardUserTagEntity h where h.board.boardId = :boardId")
    void deleteByBoard_BoardId(@Param("boardId") String boardId);

    @Query("""
            select new com.tagnote.infrastructure.persistence.board.BoardUserTagNameRow(h.board.boardId, h.userTag.name)
            from BoardUserTagEntity h
            where h.board.boardId in :boardIds
            order by h.board.boardId asc, h.boardUserTagId asc
            """)
    List<BoardUserTagNameRow> findNamesByBoardIds(@Param("boardIds") Collection<String> boardIds);

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
