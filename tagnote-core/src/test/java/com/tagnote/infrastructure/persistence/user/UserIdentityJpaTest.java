package com.tagnote.infrastructure.persistence.user;

import com.tagnote.core.domain.user.AccountProvider;
import com.tagnote.core.common.CommonRedisTemplate;
import com.tagnote.core.domain.user.Role;
import com.tagnote.core.domain.user.UserEntity;
import com.tagnote.core.domain.user.UserStatus;
import com.tagnote.core.domain.user.repository.UserRepository;
import jakarta.persistence.PersistenceException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ContextConfiguration(classes = UserIdentityJpaTest.Configuration.class)
@ActiveProfiles("local")
@TestPropertySource(properties = {"spring.sql.init.mode=never", "spring.jpa.hibernate.ddl-auto=create-drop"})
class UserIdentityJpaTest {
    @TestConfiguration
    @EntityScan(basePackageClasses = UserEntity.class)
    @EnableJpaRepositories(basePackageClasses = UserRepository.class)
    static class Configuration {
    }

    @Autowired UserRepository users;
    @Autowired TestEntityManager entityManager;
    @MockBean CommonRedisTemplate redis;

    @Test
    void Google_legacy_key를_subject로_저장하고_정규화_email로_조회한다() {
        UserEntity saved = users.saveAndFlush(google("sub-1", " User@Example.com ", "one"));
        entityManager.clear();

        UserEntity found = users.findByEmail("user@example.com").orElseThrow();
        assertThat(found.getUserId()).isEqualTo(saved.getUserId());
        assertThat(found.getUserKey()).isEqualTo("sub-1");
        assertThat(found.getProvider()).isEqualTo(AccountProvider.GOOGLE);
        assertThat(found.getSubject()).isEqualTo("sub-1");
        assertThat(users.findByProviderAndSubject(AccountProvider.GOOGLE, "sub-1"))
                .contains(found);
    }

    @Test
    void 정규화_email_중복은_DB가_거부한다() {
        users.saveAndFlush(google("sub-1", " User@Example.com ", "one"));
        assertThatThrownBy(() -> users.saveAndFlush(google("sub-2", "user@example.com", "two")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void Google_subject_중복은_user_key가_달라도_DB가_거부한다() {
        users.saveAndFlush(google("sub-1", "one@example.com", "one"));
        users.saveAndFlush(google("sub-2", "two@example.com", "two"));
        assertThatThrownBy(() -> entityManager.getEntityManager()
                .createNativeQuery("update users set subject = 'sub-1' where user_key = 'sub-2'")
                .executeUpdate())
                .isInstanceOf(PersistenceException.class);
    }

    @Test
    void user_key_중복은_DB가_거부한다() {
        users.saveAndFlush(google("sub-1", "one@example.com", "one"));
        assertThatThrownBy(() -> users.saveAndFlush(google("sub-1", "two@example.com", "two")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void nickname_중복은_DB가_거부한다() {
        users.saveAndFlush(google("sub-1", "one@example.com", "same"));
        assertThatThrownBy(() -> users.saveAndFlush(google("sub-2", "two@example.com", "same")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void nickname_변경은_새_조회에_반영된다() {
        UserEntity user = users.saveAndFlush(google("sub-1", "one@example.com", "before"));
        user.updateNickname("after");
        users.flush();
        entityManager.clear();

        assertThat(users.findByUserId(user.getUserId()).orElseThrow().getNickname()).isEqualTo("after");
    }

    @Test
    void nickname_변경의_중복도_DB가_거부한다() {
        users.saveAndFlush(google("sub-1", "one@example.com", "taken"));
        UserEntity other = users.saveAndFlush(google("sub-2", "two@example.com", "before"));
        other.updateNickname("taken");

        assertThatThrownBy(() -> users.flush()).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void nickname_컬럼은_20자를_허용하고_21자를_거부한다() {
        String twenty = "가".repeat(20);
        users.saveAndFlush(google("sub-1", "one@example.com", twenty));
        assertThatThrownBy(() -> users.saveAndFlush(google("sub-2", "two@example.com", "가".repeat(21))))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void LOCAL_subject는_null이고_Google_email과_충돌한다() {
        users.saveAndFlush(google("sub-1", "one@example.com", "one"));
        UserEntity local = users.saveAndFlush(UserEntity.builder()
                .userKey("local-1").provider(AccountProvider.LOCAL)
                .email("two@example.com").nickname("two")
                .role(Role.ROLE_USER).userStatus(UserStatus.ACTIVE).build());
        assertThat(local.getSubject()).isNull();
        assertThatThrownBy(() -> users.saveAndFlush(UserEntity.builder()
                .userKey("local-2").provider(AccountProvider.LOCAL)
                .email(" ONE@EXAMPLE.COM ").nickname("three")
                .role(Role.ROLE_USER).userStatus(UserStatus.ACTIVE).build()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void withdrawn_google_identity_is_released_without_reusing_user_id() {
        UserEntity former = users.saveAndFlush(google("sub-1", "one@example.com", "before"));
        Long formerId = former.getUserId();
        former.withdraw();
        users.flush();
        entityManager.clear();

        UserEntity rejoined = users.saveAndFlush(google("sub-1", "one@example.com", "after"));
        assertThat(rejoined.getUserId()).isNotEqualTo(formerId);
        assertThat(users.findByUserId(formerId).orElseThrow().getDisplayNickname())
                .isEqualTo("탈퇴한 사용자");
        assertThat(users.findByUserId(formerId).orElseThrow().getPasswordHash()).isNull();
    }

    @Test
    void withdrawn_local_email_can_be_reused_by_new_account() {
        UserEntity former = users.saveAndFlush(UserEntity.builder()
                .userKey("local-1").provider(AccountProvider.LOCAL)
                .email("local@example.com").passwordHash("old-hash").nickname("old")
                .role(Role.ROLE_USER).userStatus(UserStatus.ACTIVE).build());
        Long formerId = former.getUserId();
        former.withdraw();
        users.flush();
        entityManager.clear();

        UserEntity rejoined = users.saveAndFlush(UserEntity.builder()
                .userKey("local-2").provider(AccountProvider.LOCAL)
                .email("local@example.com").passwordHash("new-hash").nickname("new")
                .role(Role.ROLE_USER).userStatus(UserStatus.ACTIVE).build());
        assertThat(rejoined.getUserId()).isNotEqualTo(formerId);
        assertThat(users.findByUserId(formerId).orElseThrow().getSubject()).isNull();
        assertThat(users.findByUserId(formerId).orElseThrow().getPasswordHash()).isNull();
    }

    private UserEntity google(String key, String email, String nickname) {
        return UserEntity.builder().userKey(key).email(email).nickname(nickname)
                .role(Role.ROLE_USER).userStatus(UserStatus.ACTIVE).build();
    }
}
