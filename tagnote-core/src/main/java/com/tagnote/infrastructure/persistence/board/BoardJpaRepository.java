package com.tagnote.infrastructure.persistence.board;

import com.tagnote.core.domain.board.BoardEntity;
import com.tagnote.core.domain.board.BoardStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BoardJpaRepository extends JpaRepository<BoardEntity, String> {

    @EntityGraph(attributePaths = {"userEntity", "track", "album"})
    Optional<BoardEntity> findByBoardIdAndStatus(String boardId, BoardStatus status);

    @EntityGraph(attributePaths = {"userEntity"})
    @Query("select b from BoardEntity b where b.boardId = :boardId and b.status = :status")
    Optional<BoardEntity> findLikeTarget(@Param("boardId") String boardId, @Param("status") BoardStatus status);

    @EntityGraph(attributePaths = {"userEntity", "track", "album"})
    Page<BoardEntity> findByStatusAndTrack_SpotifyIdOrderByCreatedAtDesc(
            BoardStatus status, String spotifyId, Pageable pageable
    );

    @EntityGraph(attributePaths = {"userEntity", "track", "album"})
    Page<BoardEntity> findByStatusAndTrack_SpotifyIdOrderByLikeCountDesc(
            BoardStatus status, String spotifyId, Pageable pageable
    );

    @EntityGraph(attributePaths = {"userEntity", "track", "album"})
    Page<BoardEntity> findByStatusAndAlbum_SpotifyIdOrderByCreatedAtDesc(
            BoardStatus status, String spotifyId, Pageable pageable
    );

    @EntityGraph(attributePaths = {"userEntity", "track", "album"})
    Page<BoardEntity> findByStatusAndAlbum_SpotifyIdOrderByLikeCountDesc(
            BoardStatus status, String spotifyId, Pageable pageable
    );

    int countByTrack_SpotifyIdAndStatus(String spotifyId, BoardStatus status);

    int countByAlbum_SpotifyIdAndStatus(String spotifyId, BoardStatus status);

    boolean existsByBoardIdAndUserEntity_UserIdAndStatus(String boardId, Long userId, BoardStatus status);

    @Query("""
            select distinct b
            from BoardUserTagEntity matched
            join matched.board b
            join fetch b.userEntity
            left join fetch b.track
            left join fetch b.album
            where matched.userTag.name = :name
              and b.status = :status
            order by b.createdAt desc
            """)
    List<BoardEntity> findBoardsByUserTagName(
            @Param("name") String name,
            @Param("status") BoardStatus status
    );

    @EntityGraph(attributePaths = {"userEntity", "track", "album"})
    Page<BoardEntity> findByTrack_SpotifyId(String spotifyId, Pageable pageable);

    @Modifying
    @Query("update BoardEntity b set b.likeCount = b.likeCount + :value where b.boardId = :boardId")
    int updateLikeCount(@Param("boardId") String boardId, @Param("value") int value);
}
