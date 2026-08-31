package com.tagnote.infrastructure.persistence.taxonomy;

import com.tagnote.domain.taxonomy.alias.AliasSource;
import com.tagnote.domain.taxonomy.alias.AliasStatus;
import com.tagnote.domain.taxonomy.alias.TagAliasEntity;
import com.tagnote.domain.taxonomy.matching.TagNameNormalizer;
import com.tagnote.domain.taxonomy.tag.TagEntity;
import com.tagnote.domain.taxonomy.tag.TagStatus;
import com.tagnote.domain.taxonomy.tag.TagType;
import jakarta.persistence.PersistenceException;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ContextConfiguration(classes = TaxonomyJpaTestConfiguration.class)
@Import({TagNameNormalizer.class})
@ActiveProfiles("local")
@TestPropertySource(properties = {
        "spring.sql.init.mode=never",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.generate_statistics=true"
})
class TaxonomyJpaRepositoryTest {

    @Autowired
    private TagJpaRepository tagRepository;

    @Autowired
    private TagAliasJpaRepository aliasRepository;

    @Autowired
    private TagNameNormalizer normalizer;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void Tag와_Alias를_저장하고_조회한다() {
        TagEntity tag = tagRepository.save(tag("ambient", TagStatus.ACTIVE));
        aliasRepository.save(alias(tag, "Ambient", AliasStatus.APPROVED));
        entityManager.flush();
        entityManager.clear();

        List<TagAliasEntity> aliases = aliasRepository.findApprovedByNormalizedAliases(List.of("ambient"));
        entityManager.clear();

        assertThat(aliases).hasSize(1);
        assertThat(aliases.get(0).getAlias()).isEqualTo("Ambient");
        assertThat(aliases.get(0).getTag().getSlug()).isEqualTo("ambient");
    }

    @Test
    void slug_unique_제약이_중복을_거부한다() {
        tagRepository.saveAndFlush(tag("ambient", TagStatus.ACTIVE));

        assertThatThrownBy(() -> tagRepository.saveAndFlush(tag("ambient", TagStatus.CANDIDATE)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void 같은_Tag의_normalized_alias_unique_제약이_중복을_거부한다() {
        TagEntity tag = tagRepository.saveAndFlush(tag("ambient", TagStatus.ACTIVE));
        aliasRepository.saveAndFlush(alias(tag, "Ambient", AliasStatus.APPROVED));

        assertThatThrownBy(() -> aliasRepository.saveAndFlush(alias(tag, "AMBIENT", AliasStatus.PENDING)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void 존재하지_않는_Tag_FK를_거부한다() {
        assertThatThrownBy(() -> entityManager.getEntityManager()
                .createNativeQuery("""
                        insert into tag_alias (tag_id, alias, normalized_alias, source, status)
                        values (999999, 'Ambient', 'ambient', 'ADMIN', 'APPROVED')
                        """)
                .executeUpdate())
                .isInstanceOf(PersistenceException.class);
    }

    @Test
    void approved_alias만_normalized_name_목록으로_한번에_조회하고_Tag를_함께_적재한다() {
        TagEntity ambient = tagRepository.save(tag("ambient", TagStatus.ACTIVE));
        TagEntity house = tagRepository.save(tag("house", TagStatus.ACTIVE));
        TagEntity techno = tagRepository.save(tag("techno", TagStatus.ACTIVE));
        aliasRepository.saveAll(List.of(
                alias(ambient, "Ambient", AliasStatus.APPROVED),
                alias(house, "House", AliasStatus.APPROVED),
                alias(techno, "Techno", AliasStatus.PENDING)
        ));
        entityManager.flush();
        entityManager.clear();

        Statistics statistics = entityManager.getEntityManager()
                .getEntityManagerFactory()
                .unwrap(SessionFactory.class)
                .getStatistics();
        statistics.clear();

        List<TagAliasEntity> aliases = aliasRepository.findApprovedByNormalizedAliases(
                List.of("ambient", "house", "techno")
        );
        assertThat(aliases).extracting(TagAliasEntity::getNormalizedAlias)
                .containsExactlyInAnyOrder("ambient", "house");
        assertThat(aliases).extracting(alias -> alias.getTag().getSlug())
                .containsExactlyInAnyOrder("ambient", "house");
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(1L);
    }

    @Test
    void 같은_normalized_alias를_서로_다른_Tag에_저장할_수_있다() {
        TagEntity garageRock = tagRepository.save(tag("garage-rock", TagStatus.ACTIVE));
        TagEntity ukGarage = tagRepository.save(tag("uk-garage", TagStatus.ACTIVE));

        aliasRepository.saveAllAndFlush(List.of(
                alias(garageRock, "Garage", AliasStatus.APPROVED),
                alias(ukGarage, "Garage", AliasStatus.APPROVED)
        ));

        assertThat(aliasRepository.findApprovedByNormalizedAliases(List.of("garage"))).hasSize(2);
    }

    private TagEntity tag(String slug, TagStatus status) {
        return TagEntity.create(slug, slug, TagType.GENRE, status, null);
    }

    private TagAliasEntity alias(TagEntity tag, String rawAlias, AliasStatus status) {
        return TagAliasEntity.create(tag, rawAlias, normalizer.normalize(rawAlias), AliasSource.ADMIN, status);
    }
}
