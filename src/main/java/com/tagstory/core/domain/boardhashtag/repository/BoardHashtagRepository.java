package com.tagstory.core.domain.boardhashtag.repository;

import com.tagstory.core.domain.board.BoardEntity;
import com.tagstory.core.domain.boardhashtag.BoardHashtagEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BoardHashtagRepository extends JpaRepository<BoardHashtagEntity, Long> {

    //@Query("SELECT h FROM BoardHashtagEntity h JOIN FETCH h.hashtag WHERE h.board.boardId = :boardId")
    @Query("SELECT h.hashtag.name FROM BoardHashtagEntity h WHERE h.board.boardId = :boardId")
    List<String> findHashtagNameByBoardId(@Param("boardId") String boardId);

}
