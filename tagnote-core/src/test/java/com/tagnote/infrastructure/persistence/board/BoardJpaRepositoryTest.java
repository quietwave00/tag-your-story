package com.tagnote.infrastructure.persistence.board;

import com.tagnote.core.domain.board.BoardEntity;
import com.tagnote.core.domain.board.BoardStatus;
import com.tagnote.core.domain.boardusertag.BoardUserTagEntity;
import com.tagnote.core.domain.boardusertag.repository.BoardUserTagRepository;
import com.tagnote.core.domain.user.Role;
import com.tagnote.core.domain.user.UserEntity;
import com.tagnote.core.domain.user.UserStatus;
import com.tagnote.core.domain.usertag.UserTagEntity;
import com.tagnote.domain.catalog.album.AlbumEntity;
import com.tagnote.domain.catalog.track.TrackEntity;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceException;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ContextConfiguration(classes = BoardJpaTestConfiguration.class)
@ActiveProfiles("local")
@TestPropertySource(properties = {
        "spring.sql.init.mode=never",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.generate_statistics=true"
})
class BoardJpaRepositoryTest {

    @Autowired
    private BoardJpaRepository boardRepository;
    @Autowired
    private BoardUserTagRepository boardUserTagRepository;
    @Autowired
    private TestEntityManager entityManager;
    @Autowired
    private EntityManagerFactory entityManagerFactory;

    private UserEntity writer;
    private AlbumEntity album;
    private TrackEntity track;

    @BeforeEach
    void setUp() {
        writer = entityManager.persistAndFlush(UserEntity.builder()
                .userKey("writer")
                .email("writer@test.com")
                .nickname("Writer")
                .role(Role.ROLE_USER)
                .userStatus(UserStatus.ACTIVE)
                .build());
        album = entityManager.persistAndFlush(AlbumEntity.create("Album", "spotify-album", 2026));
        track = entityManager.persistAndFlush(TrackEntity.create(
                "Track", "spotify-track", null, 180_000, album
        ));
    }

    @Test
    void Track과_Album_목록은_POST와_대상_FK를_각각_필터링한다() {
        BoardEntity trackPost = boardRepository.saveAndFlush(
                BoardEntity.createForTrack("track-post", writer, track)
        );
        BoardEntity removedTrack = BoardEntity.createForTrack("track-removed", writer, track);
        removedTrack.delete();
        boardRepository.saveAndFlush(removedTrack);
        BoardEntity albumPost = boardRepository.saveAndFlush(
                BoardEntity.createForAlbum("album-post", writer, album)
        );
        entityManager.clear();

        Page<BoardEntity> tracks = boardRepository.findByStatusAndTrack_SpotifyIdOrderByCreatedAtDesc(
                BoardStatus.POST, "spotify-track", PageRequest.of(0, 8)
        );
        Page<BoardEntity> albums = boardRepository.findByStatusAndAlbum_SpotifyIdOrderByCreatedAtDesc(
                BoardStatus.POST, "spotify-album", PageRequest.of(0, 8)
        );

        assertThat(tracks.getContent()).extracting(BoardEntity::getBoardId)
                .containsExactly(trackPost.getBoardId());
        assertThat(albums.getContent()).extracting(BoardEntity::getBoardId)
                .containsExactly(albumPost.getBoardId());
        assertThat(boardRepository.countByTrack_SpotifyIdAndStatus("spotify-track", BoardStatus.POST))
                .isEqualTo(1);
        assertThat(boardRepository.countByAlbum_SpotifyIdAndStatus("spotify-album", BoardStatus.POST))
                .isEqualTo(1);
    }

    @Test
    void page_base_query는_writer와_subject를_추가_SELECT없이_읽는다() {
        boardRepository.saveAndFlush(BoardEntity.createForTrack("first", writer, track));
        boardRepository.saveAndFlush(BoardEntity.createForTrack("second", writer, track));
        entityManager.clear();
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();

        Page<BoardEntity> page = boardRepository.findByStatusAndTrack_SpotifyIdOrderByCreatedAtDesc(
                BoardStatus.POST, "spotify-track", PageRequest.of(0, 1)
        );
        page.getContent().forEach(board -> {
            board.getUserEntity().getNickname();
            board.getTrack().getSpotifyId();
        });

        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(2L);
    }

    @Test
    void Track과_Album_page는_CREATED_AT과_LIKE_내림차순을_적용한다() {
        BoardEntity oldTrack = boardRepository.saveAndFlush(
                BoardEntity.createForTrack("old-track", writer, track)
        );
        BoardEntity newTrack = boardRepository.saveAndFlush(
                BoardEntity.createForTrack("new-track", writer, track)
        );
        BoardEntity oldAlbum = boardRepository.saveAndFlush(
                BoardEntity.createForAlbum("old-album", writer, album)
        );
        BoardEntity newAlbum = boardRepository.saveAndFlush(
                BoardEntity.createForAlbum("new-album", writer, album)
        );
        setSortValues(oldTrack.getBoardId(), LocalDateTime.of(2026, 1, 1, 0, 0), 20);
        setSortValues(newTrack.getBoardId(), LocalDateTime.of(2026, 2, 1, 0, 0), 10);
        setSortValues(oldAlbum.getBoardId(), LocalDateTime.of(2026, 1, 1, 0, 0), 20);
        setSortValues(newAlbum.getBoardId(), LocalDateTime.of(2026, 2, 1, 0, 0), 10);
        entityManager.clear();

        assertThat(boardRepository.findByStatusAndTrack_SpotifyIdOrderByCreatedAtDesc(
                BoardStatus.POST, "spotify-track", PageRequest.of(0, 8)
        ).getContent()).extracting(BoardEntity::getBoardId)
                .containsExactly(newTrack.getBoardId(), oldTrack.getBoardId());
        assertThat(boardRepository.findByStatusAndTrack_SpotifyIdOrderByLikeCountDesc(
                BoardStatus.POST, "spotify-track", PageRequest.of(0, 8)
        ).getContent()).extracting(BoardEntity::getBoardId)
                .containsExactly(oldTrack.getBoardId(), newTrack.getBoardId());
        assertThat(boardRepository.findByStatusAndAlbum_SpotifyIdOrderByCreatedAtDesc(
                BoardStatus.POST, "spotify-album", PageRequest.of(0, 8)
        ).getContent()).extracting(BoardEntity::getBoardId)
                .containsExactly(newAlbum.getBoardId(), oldAlbum.getBoardId());
        assertThat(boardRepository.findByStatusAndAlbum_SpotifyIdOrderByLikeCountDesc(
                BoardStatus.POST, "spotify-album", PageRequest.of(0, 8)
        ).getContent()).extracting(BoardEntity::getBoardId)
                .containsExactly(oldAlbum.getBoardId(), newAlbum.getBoardId());
    }

    @Test
    void UserTag_exact_name_조회는_Track과_Album_Board를_distinct로_반환한다() {
        BoardEntity trackBoard = boardRepository.saveAndFlush(
                BoardEntity.createForTrack("track", writer, track)
        );
        BoardEntity albumBoard = boardRepository.saveAndFlush(
                BoardEntity.createForAlbum("album", writer, album)
        );
        UserTagEntity exact = entityManager.persistAndFlush(UserTagEntity.create(writer, "Favorite"));
        UserTagEntity differentCase = entityManager.persistAndFlush(UserTagEntity.create(writer, "FAVORITE"));
        boardUserTagRepository.saveAllAndFlush(List.of(
                BoardUserTagEntity.of(trackBoard, exact),
                BoardUserTagEntity.of(albumBoard, exact),
                BoardUserTagEntity.of(albumBoard, differentCase)
        ));
        entityManager.clear();

        List<BoardEntity> result = boardRepository.findBoardsByUserTagName("Favorite", BoardStatus.POST);

        assertThat(result).extracting(BoardEntity::getBoardId)
                .containsExactlyInAnyOrder(trackBoard.getBoardId(), albumBoard.getBoardId());
    }

    @Test
    void DB_check는_Track과_Album_FK가_둘다_있는_Board를_거부한다() {
        assertThatThrownBy(() -> entityManager.getEntityManager().createNativeQuery("""
                insert into board
                    (board_id, created_at, updated_at, content, count, status,
                     track_id, album_id, user_id, like_count)
                values
                    ('invalid-both', current_timestamp, current_timestamp, 'content', 0, 'POST',
                     :trackId, :albumId, :userId, 0)
                """)
                .setParameter("trackId", track.getTrackId())
                .setParameter("albumId", album.getAlbumId())
                .setParameter("userId", writer.getUserId())
                .executeUpdate()).isInstanceOf(PersistenceException.class);
    }

    @Test
    void DB_check는_대상_FK가_없는_Board를_거부한다() {
        assertThatThrownBy(() -> entityManager.getEntityManager().createNativeQuery("""
                insert into board
                    (board_id, created_at, updated_at, content, count, status,
                     track_id, album_id, user_id, like_count)
                values
                    ('invalid-none', current_timestamp, current_timestamp, 'content', 0, 'POST',
                     null, null, :userId, 0)
                """)
                .setParameter("userId", writer.getUserId())
                .executeUpdate()).isInstanceOf(PersistenceException.class);
    }

    private void setSortValues(String boardId, LocalDateTime createdAt, int likeCount) {
        entityManager.getEntityManager().createNativeQuery("""
                update board
                set created_at = :createdAt, like_count = :likeCount
                where board_id = :boardId
                """)
                .setParameter("createdAt", createdAt)
                .setParameter("likeCount", likeCount)
                .setParameter("boardId", boardId)
                .executeUpdate();
    }
}
