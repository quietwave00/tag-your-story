package com.tagnote.infrastructure.persistence.like;

import com.tagnote.core.domain.board.BoardStatus;
import com.tagnote.core.domain.like.LikeEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LikeJpaRepository extends JpaRepository<LikeEntity, Long> {
    boolean existsByBoard_BoardIdAndUser_UserId(String boardId, Long userId);

    @Modifying
    @Query("""
            delete from LikeEntity l
            where l.board in (select b from BoardEntity b where b.boardId = :boardId)
              and l.user in (select u from UserEntity u where u.userId = :userId)
            """)
    int deleteByBoardIdAndUserId(@Param("boardId") String boardId, @Param("userId") Long userId);

    @Query(value = """
            select l from LikeEntity l
            join fetch l.board b
            join fetch b.userEntity
            where l.user.userId = :userId and b.status = :status
            order by l.createdAt desc, l.likeId desc
            """, countQuery = """
            select count(l) from LikeEntity l
            join l.board b
            where l.user.userId = :userId and b.status = :status
            """)
    Page<LikeEntity> findLikedBoards(@Param("userId") Long userId,
                                     @Param("status") BoardStatus status, Pageable pageable);
}
