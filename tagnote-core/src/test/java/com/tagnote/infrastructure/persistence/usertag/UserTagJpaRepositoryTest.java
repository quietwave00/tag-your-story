package com.tagnote.infrastructure.persistence.usertag;

import com.tagnote.core.domain.board.BoardEntity;
import com.tagnote.core.domain.board.BoardStatus;
import com.tagnote.core.domain.board.repository.BoardRepository;
import com.tagnote.core.domain.boardusertag.BoardUserTagEntity;
import com.tagnote.core.domain.boardusertag.repository.BoardUserTagRepository;
import com.tagnote.core.domain.user.Role;
import com.tagnote.core.domain.user.UserEntity;
import com.tagnote.core.domain.user.UserStatus;
import com.tagnote.core.domain.usertag.UserTagEntity;
import com.tagnote.core.domain.usertag.repository.UserTagRepository;
import jakarta.persistence.PersistenceException;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ContextConfiguration(classes = UserTagJpaTestConfiguration.class)
@ActiveProfiles("local")
@TestPropertySource(properties = {
        "spring.sql.init.mode=never",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.generate_statistics=true"
})
class UserTagJpaRepositoryTest {

    @Autowired
    private UserTagRepository userTagRepository;

    @Autowired
    private BoardRepository boardRepository;

    @Autowired
    private BoardUserTagRepository boardUserTagRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void 같은_owner의_정확히_같은_name_unique가_중복을_거부한다() {
        UserEntity owner = persistUser("owner-1");
        userTagRepository.saveAndFlush(UserTagEntity.create(owner, "Jazz"));

        assertThatThrownBy(() -> userTagRepository.saveAndFlush(
                UserTagEntity.create(owner, "Jazz")
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void 같은_owner도_입력값이_다른_name은_별도로_저장한다() {
        UserEntity owner = persistUser("variant-owner");

        userTagRepository.saveAllAndFlush(List.of(
                UserTagEntity.create(owner, "Jazz"),
                UserTagEntity.create(owner, "jazz"),
                UserTagEntity.create(owner, "  Jazz  ")
        ));

        assertThat(userTagRepository.count()).isEqualTo(3L);
    }

    @Test
    void 다른_owner는_정확히_같은_name을_각자_저장한다() {
        UserEntity firstOwner = persistUser("owner-1");
        UserEntity secondOwner = persistUser("owner-2");

        userTagRepository.saveAllAndFlush(List.of(
                UserTagEntity.create(firstOwner, "Jazz"),
                UserTagEntity.create(secondOwner, "Jazz")
        ));

        assertThat(userTagRepository.count()).isEqualTo(2L);
    }

    @Test
    void 존재하지_않는_owner_FK를_거부한다() {
        assertThatThrownBy(() -> entityManager.getEntityManager().createNativeQuery("""
                insert into user_tag
                    (user_id, name, created_at, updated_at)
                values
                    (999999, 'Jazz', current_timestamp, current_timestamp)
                """).executeUpdate())
                .isInstanceOf(PersistenceException.class);
    }

    @Test
    void 같은_Board와_UserTag_연결_unique가_중복을_거부한다() {
        UserEntity owner = persistUser("owner-1");
        UserTagEntity tag = userTagRepository.saveAndFlush(UserTagEntity.create(owner, "Jazz"));
        BoardEntity board = persistBoard(owner, "board-content", false);
        boardUserTagRepository.saveAndFlush(BoardUserTagEntity.of(board, tag));

        assertThatThrownBy(() -> boardUserTagRepository.saveAndFlush(BoardUserTagEntity.of(board, tag)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void 공개조회는_exact_name이_같은_모든_owner의_POST_Board를_distinct로_조회한다() {
        UserEntity firstOwner = persistUser("owner-1");
        UserEntity secondOwner = persistUser("owner-2");
        UserTagEntity firstJazz = userTagRepository.save(UserTagEntity.create(firstOwner, "Jazz"));
        UserTagEntity firstFavorite = userTagRepository.save(UserTagEntity.create(firstOwner, "Favorite"));
        UserTagEntity secondJazz = userTagRepository.save(UserTagEntity.create(secondOwner, "Jazz"));
        UserTagEntity differentCase = userTagRepository.save(UserTagEntity.create(secondOwner, "JAZZ"));
        userTagRepository.flush();

        persistBoardWithTags(firstOwner, false, firstJazz, firstFavorite);
        persistBoardWithTags(secondOwner, false, secondJazz);
        persistBoardWithTags(secondOwner, false, differentCase);
        persistBoardWithTags(firstOwner, true, firstJazz);
        entityManager.flush();
        entityManager.clear();

        Statistics statistics = entityManager.getEntityManager().getEntityManagerFactory()
                .unwrap(SessionFactory.class)
                .getStatistics();
        statistics.clear();

        List<BoardEntity> boards = boardRepository.findBoardsByUserTagName("Jazz", BoardStatus.POST);
        boards.forEach(board -> {
            board.getUserEntity().getNickname();
            board.getBoardUserTagEntityList().forEach(join -> join.getUserTag().getName());
        });

        assertThat(boards).hasSize(2);
        assertThat(boards).allMatch(board -> board.getStatus() == BoardStatus.POST);
        assertThat(boards).extracting(board -> board.getUserEntity().getUserKey())
                .containsExactlyInAnyOrder("owner-1", "owner-2");
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(1L);
    }

    private UserEntity persistUser(String userKey) {
        UserEntity user = UserEntity.builder()
                .userKey(userKey)
                .email(userKey + "@test.com")
                .nickname(userKey)
                .role(Role.ROLE_USER)
                .userStatus(UserStatus.ACTIVE)
                .build();
        return entityManager.persistAndFlush(user);
    }

    private BoardEntity persistBoard(UserEntity owner, String content, boolean removed) {
        BoardEntity board = BoardEntity.create(content, "track-1");
        board.addUser(owner);
        if (removed) {
            board.delete();
        }
        return boardRepository.saveAndFlush(board);
    }

    private void persistBoardWithTags(UserEntity owner, boolean removed, UserTagEntity... tags) {
        BoardEntity board = BoardEntity.create("content", "track-1");
        board.addUser(owner);
        if (removed) {
            board.delete();
        }
        board.addBoardUserTagList(
                java.util.Arrays.stream(tags).map(tag -> BoardUserTagEntity.of(board, tag)).toList()
        );
        boardRepository.save(board);
    }
}
