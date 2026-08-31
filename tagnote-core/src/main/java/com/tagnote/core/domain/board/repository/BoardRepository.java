package com.tagnote.core.domain.board.repository;

import com.tagnote.core.domain.board.BoardEntity;
import com.tagnote.core.domain.board.BoardStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BoardRepository extends JpaRepository<BoardEntity, Long> {
    Page<BoardEntity> findByStatusAndTrackIdOrderByBoardIdDesc(BoardStatus post, String trackId, Pageable pageable);

    Page<BoardEntity> findByTrackId(String trackId, Pageable pageable);

    Optional<BoardEntity> findByBoardIdAndStatus(String boardId, BoardStatus status);

    Optional<BoardEntity> findByBoardId(String boardId);

    int countByTrackIdAndStatus(String trackId, BoardStatus status);

    @Query("""
            SELECT DISTINCT b
            FROM BoardUserTagEntity matched
            JOIN matched.board b
            JOIN FETCH b.userEntity
            LEFT JOIN FETCH b.boardUserTagEntityList boardTag
            LEFT JOIN FETCH boardTag.userTag
            WHERE matched.userTag.normalizedName = :normalizedName
              AND b.status = :status
            ORDER BY b.createdAt DESC
            """)
    List<BoardEntity> findBoardsByNormalizedUserTagName(
            @Param("normalizedName") String normalizedName,
            @Param("status") BoardStatus status
    );

    Optional<BoardEntity> findByBoardIdAndUserEntity_UserId(String BoardId, Long userId);

    Page<BoardEntity> findByStatusAndTrackIdOrderByCreatedAtDesc(BoardStatus status, String trackId, PageRequest of);

    Page<BoardEntity> findByStatusAndTrackIdOrderByLikeCountDesc(BoardStatus status, String trackId, PageRequest of);

    @Modifying
    @Query("UPDATE BoardEntity b set b.likeCount = b.likeCount + :value WHERE b.boardId = :boardId")
    void updateLikeCount(String boardId, int value);
}
