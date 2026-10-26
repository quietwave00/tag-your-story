package com.tagnote.infrastructure.persistence.like;

import com.tagnote.core.domain.board.BoardEntity;
import com.tagnote.core.domain.board.BoardStatus;
import com.tagnote.core.domain.like.LikeEntity;
import com.tagnote.core.domain.user.Role;
import com.tagnote.core.domain.user.UserEntity;
import com.tagnote.core.domain.user.UserStatus;
import com.tagnote.domain.catalog.album.AlbumEntity;
import com.tagnote.domain.catalog.track.TrackEntity;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ContextConfiguration(classes = LikeJpaTestConfiguration.class)
@ActiveProfiles("local")
@TestPropertySource(properties = {"spring.sql.init.mode=never", "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.generate_statistics=true"})
class LikeJpaRepositoryTest {
    @Autowired LikeJpaRepository likes;
    @Autowired TestEntityManager em;
    @Autowired EntityManagerFactory emf;

    private UserEntity user(String key) {
        return em.persistAndFlush(UserEntity.builder().userKey(key).email(key + "@test.com")
                .nickname(key).role(Role.ROLE_USER).userStatus(UserStatus.ACTIVE).build());
    }

    private TrackEntity track() {
        AlbumEntity album = em.persistAndFlush(AlbumEntity.create("album", "album-like", 2026));
        return em.persistAndFlush(TrackEntity.create("track", "track-like", null, 120000, album));
    }

    @Test
    void DB_unique는_같은_user_board의_두번째_insert를_막는다() {
        UserEntity liker = user("liker");
        BoardEntity board = em.persistAndFlush(BoardEntity.createForTrack("content", liker, track()));
        likes.saveAndFlush(LikeEntity.createLike(liker, board));

        assertThatThrownBy(() -> likes.save(LikeEntity.createLike(liker, board)))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }

    @Test
    void 목록은_사용자와_POST를_필터하고_작성자를_추가_SELECT없이_읽는다() {
        UserEntity liker = user("liker");
        UserEntity other = user("other");
        UserEntity writer = user("writer");
        TrackEntity track = track();
        for (int i = 0; i < 9; i++) {
            BoardEntity board = em.persistAndFlush(BoardEntity.createForTrack("content-" + i, writer, track));
            likes.saveAndFlush(LikeEntity.createLike(liker, board));
        }
        BoardEntity removed = BoardEntity.createForTrack("removed", writer, track);
        removed.delete();
        removed = em.persistAndFlush(removed);
        likes.saveAndFlush(LikeEntity.createLike(liker, removed));
        BoardEntity others = em.persistAndFlush(BoardEntity.createForTrack("others", writer, track));
        likes.saveAndFlush(LikeEntity.createLike(other, others));
        writer.withdraw();
        em.flush();
        em.getEntityManager().createQuery("update LikeEntity l set l.createdAt = :createdAt")
                .setParameter("createdAt", LocalDateTime.of(2026, 1, 1, 0, 0))
                .executeUpdate();
        em.clear();
        Statistics statistics = emf.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();

        Page<LikeEntity> page = likes.findLikedBoards(liker.getUserId(), BoardStatus.POST, PageRequest.of(0, 8));
        assertThat(page.getContent()).hasSize(8);
        assertThat(page.getTotalElements()).isEqualTo(9);
        assertThat(page.getContent()).allSatisfy(like ->
                assertThat(like.getBoard().getUserEntity().getDisplayNickname()).isEqualTo("탈퇴한 사용자"));
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(2L);
        assertThat(page.getContent()).extracting(LikeEntity::getLikeId).isSortedAccordingTo(
                (left, right) -> right.compareTo(left));
        Page<LikeEntity> secondPage = likes.findLikedBoards(liker.getUserId(), BoardStatus.POST, PageRequest.of(1, 8));
        assertThat(secondPage.getContent()).hasSize(1);
    }
}
