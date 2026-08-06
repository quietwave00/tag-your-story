package com.tagnote.core.domain.like.repository;

import com.tagnote.core.domain.board.BoardEntity;
import com.tagnote.core.domain.like.LikeEntity;
import com.tagnote.core.domain.user.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LikeRepository extends JpaRepository<LikeEntity, Long> {
    int countByBoard(BoardEntity board);

    void deleteByBoardAndUser(BoardEntity board, UserEntity user);

    void countByBoard_BoardId_AndUser_UserId(String boardId, Long userId);

    Optional<LikeEntity> findByBoard_BoardId_AndUser_UserId(String boardId, Long userId);

    void deleteByBoard_BoardId_AndUser_UserId(String boardId, Long userId);
}
