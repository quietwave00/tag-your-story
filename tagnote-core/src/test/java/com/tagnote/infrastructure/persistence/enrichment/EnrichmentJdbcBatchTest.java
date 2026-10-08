package com.tagnote.infrastructure.persistence.enrichment;

import com.tagnote.domain.enrichment.assertion.AssertionSource;
import com.tagnote.domain.enrichment.assertion.EvidenceType;
import com.tagnote.domain.enrichment.assertion.TagAssertionEntity;
import com.tagnote.domain.enrichment.observation.ExternalTagObservationEntity;
import com.tagnote.domain.enrichment.observation.ExternalTagSource;
import com.tagnote.domain.enrichment.subject.SubjectRef;
import com.tagnote.domain.resolution.SubjectTagResolvedEntity;
import com.tagnote.domain.taxonomy.alias.TagAliasEntity;
import com.tagnote.domain.taxonomy.matching.NormalizedTagName;
import com.tagnote.domain.taxonomy.tag.TagEntity;
import com.tagnote.domain.taxonomy.tag.TagStatus;
import com.tagnote.domain.taxonomy.tag.TagType;
import com.tagnote.infrastructure.persistence.resolution.SubjectTagResolvedJpaRepository;
import com.tagnote.infrastructure.persistence.taxonomy.TagJpaRepository;
import net.ttddyy.dsproxy.ExecutionInfo;
import net.ttddyy.dsproxy.QueryInfo;
import net.ttddyy.dsproxy.listener.QueryExecutionListener;
import net.ttddyy.dsproxy.support.ProxyDataSourceBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ContextConfiguration(classes = EnrichmentJdbcBatchTest.BatchProbeConfiguration.class)
@ActiveProfiles("local")
@TestPropertySource(properties = {
        "spring.sql.init.mode=never",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.jdbc.batch_size=50"
})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class EnrichmentJdbcBatchTest {

    private static final int ROW_COUNT = 51;

    @Autowired private ExternalTagObservationJpaRepository observationRepository;
    @Autowired private TagAssertionJpaRepository assertionRepository;
    @Autowired private SubjectTagResolvedJpaRepository resolvedRepository;
    @Autowired private TagJpaRepository tagRepository;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private JdbcBatchProbe batchProbe;

    @BeforeEach
    void resetProbe() {
        batchProbe.reset();
    }

    @Test
    @Order(1)
    void 세_sequence의_increment가_allocation_size와_같다() {
        Map<String, Long> increments = jdbcTemplate.query(
                "select * from information_schema.sequences",
                resultSet -> {
                    Map<String, Long> result = new java.util.HashMap<>();
                    while (resultSet.next()) {
                        result.put(
                                resultSet.getString("sequence_name").toLowerCase(Locale.ROOT),
                                resultSet.getLong("increment")
                        );
                    }
                    return result;
                }
        );

        assertThat(increments)
                .containsEntry("external_tag_observation_seq", 50L)
                .containsEntry("tag_assertion_seq", 50L)
                .containsEntry("subject_tag_resolved_seq", 50L);
    }

    @Test
    @Order(2)
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    void 기존_MAX_ID보다_큰_안전한_block에서_첫_ID를_생성한다() {
        TagEntity tag = tagRepository.saveAndFlush(TagEntity.create(
                "Existing ID Tag",
                "existing-id-tag",
                TagType.GENRE,
                TagStatus.ACTIVE,
                null
        ));

        jdbcTemplate.update("""
                insert into external_tag_observation
                    (observation_id, subject_type, subject_id, source, raw_name, normalized_name,
                     external_ref, status, observed_at)
                values (10000, 'TRACK', 1, 'MUSICBRAINZ', 'Existing', 'existing',
                        'existing:observation', 'NEW', current_timestamp)
                """);
        jdbcTemplate.update("""
                insert into tag_assertion
                    (assertion_id, subject_type, subject_id, tag_id, source, evidence_type,
                     confidence, status, created_at)
                values (10000, 'TRACK', 1, ?, 'MUSICBRAINZ', 'EXPLICIT_GENRE',
                        0.9, 'APPROVED', current_timestamp)
                """, tag.getTagId());
        jdbcTemplate.update("""
                insert into subject_tag_resolved
                    (resolved_id, subject_type, subject_id, tag_id, score, status,
                     resolution_reason, last_resolved_at)
                values (10000, 'TRACK', 1, ?, 0.9, 'ACTIVE', 'AUTO', current_timestamp)
                """, tag.getTagId());
        jdbcTemplate.execute(
                "alter sequence external_tag_observation_seq restart with 10051"
        );
        jdbcTemplate.execute(
                "alter sequence tag_assertion_seq restart with 10051"
        );
        jdbcTemplate.execute(
                "alter sequence subject_tag_resolved_seq restart with 10051"
        );

        ExternalTagObservationEntity observation = observationRepository.saveAndFlush(
                ExternalTagObservationEntity.createNew(
                        SubjectRef.track(2L),
                        ExternalTagSource.MUSICBRAINZ,
                        "New",
                        new NormalizedTagName("new"),
                        "new:observation"
                )
        );
        TagAssertionEntity assertion = assertionRepository.saveAndFlush(
                TagAssertionEntity.createApproved(
                        SubjectRef.track(2L),
                        tag,
                        AssertionSource.MUSICBRAINZ,
                        EvidenceType.EXPLICIT_GENRE,
                        0.9
                )
        );
        SubjectTagResolvedEntity resolved = resolvedRepository.saveAndFlush(
                SubjectTagResolvedEntity.automatic(
                        SubjectRef.track(2L),
                        tag,
                        0.9,
                        java.time.LocalDateTime.now()
                )
        );

        assertThat(observation.getObservationId()).isGreaterThan(10000L);
        assertThat(assertion.getAssertionId()).isGreaterThan(10000L);
        assertThat(resolved.getResolvedId()).isGreaterThan(10000L);
    }

    @Test
    @Order(3)
    void 서른한개_Observation_insert는_하나의_JDBC_batch로_실행한다() {
        List<ExternalTagObservationEntity> observations = observations(31, "under-limit");

        observationRepository.saveAll(observations);
        observationRepository.flush();

        assertThat(batchProbe.batchSizesFor("external_tag_observation")).containsExactly(31);
    }

    @Test
    @Order(4)
    void 쉰한개_Observation_insert를_오십개와_한개의_JDBC_batch로_실행한다() {
        List<ExternalTagObservationEntity> observations = observations(ROW_COUNT, "over-limit");

        observationRepository.saveAll(observations);
        observationRepository.flush();

        assertUniqueIds(observations.stream().map(ExternalTagObservationEntity::getObservationId).toList());
        assertThat(batchProbe.batchSizesFor("external_tag_observation")).containsExactly(50, 1);
    }

    @Test
    @Order(5)
    void 쉰한개_Assertion과_Resolved_insert도_설정_크기로_batch한다() {
        List<TagEntity> tags = new ArrayList<>();
        for (int index = 1; index <= ROW_COUNT; index++) {
            tags.add(TagEntity.create(
                    "Tag " + index,
                    "batch-tag-" + index,
                    TagType.GENRE,
                    TagStatus.ACTIVE,
                    null
            ));
        }
        tagRepository.saveAllAndFlush(tags);
        batchProbe.reset();

        List<TagAssertionEntity> assertions = new ArrayList<>();
        List<SubjectTagResolvedEntity> resolved = new ArrayList<>();
        for (int index = 0; index < ROW_COUNT; index++) {
            SubjectRef subject = SubjectRef.track(index + 1L);
            TagEntity tag = tags.get(index);
            assertions.add(TagAssertionEntity.createApproved(
                    subject,
                    tag,
                    AssertionSource.MUSICBRAINZ,
                    EvidenceType.EXPLICIT_GENRE,
                    0.9
            ));
            resolved.add(SubjectTagResolvedEntity.automatic(
                    subject,
                    tag,
                    0.9,
                    java.time.LocalDateTime.now()
            ));
        }

        assertionRepository.saveAll(assertions);
        assertionRepository.flush();
        resolvedRepository.saveAll(resolved);
        resolvedRepository.flush();

        assertUniqueIds(assertions.stream().map(TagAssertionEntity::getAssertionId).toList());
        assertUniqueIds(resolved.stream().map(SubjectTagResolvedEntity::getResolvedId).toList());
        assertThat(batchProbe.batchSizesFor("tag_assertion")).containsExactly(50, 1);
        assertThat(batchProbe.batchSizesFor("subject_tag_resolved")).containsExactly(50, 1);
    }

    private void assertUniqueIds(List<Long> ids) {
        assertThat(ids).doesNotContainNull().doesNotHaveDuplicates().hasSize(ROW_COUNT);
    }

    private List<ExternalTagObservationEntity> observations(int count, String referencePrefix) {
        List<ExternalTagObservationEntity> observations = new ArrayList<>();
        for (int index = 1; index <= count; index++) {
            observations.add(ExternalTagObservationEntity.createNew(
                    SubjectRef.track(1L),
                    ExternalTagSource.MUSICBRAINZ,
                    "Genre " + index,
                    new NormalizedTagName("genre-" + index),
                    referencePrefix + ":" + index
            ));
        }
        return observations;
    }

    @TestConfiguration
    @EntityScan(basePackageClasses = {
            TagEntity.class,
            TagAliasEntity.class,
            ExternalTagObservationEntity.class,
            TagAssertionEntity.class,
            SubjectTagResolvedEntity.class
    })
    @EnableJpaRepositories(basePackageClasses = {
            TagJpaRepository.class,
            ExternalTagObservationJpaRepository.class,
            TagAssertionJpaRepository.class,
            SubjectTagResolvedJpaRepository.class
    })
    static class BatchProbeConfiguration {

        @Bean
        static BatchProbeDataSourcePostProcessor batchProbeDataSourcePostProcessor() {
            return new BatchProbeDataSourcePostProcessor();
        }

        @Bean
        JdbcBatchProbe jdbcBatchProbe(BatchProbeDataSourcePostProcessor postProcessor) {
            return postProcessor.probe();
        }
    }

    static final class BatchProbeDataSourcePostProcessor implements BeanPostProcessor {

        private final JdbcBatchProbe probe = new JdbcBatchProbe();

        JdbcBatchProbe probe() {
            return probe;
        }

        @Override
        public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
            if (!(bean instanceof DataSource dataSource)) {
                return bean;
            }
            return ProxyDataSourceBuilder.create(dataSource)
                    .name("enrichment-batch-test")
                    .listener(probe)
                    .build();
        }
    }

    static final class JdbcBatchProbe implements QueryExecutionListener {

        private final List<BatchExecution> executions = new ArrayList<>();

        @Override
        public void beforeQuery(ExecutionInfo executionInfo, List<QueryInfo> queryInfoList) {
        }

        @Override
        public synchronized void afterQuery(ExecutionInfo executionInfo, List<QueryInfo> queryInfoList) {
            if (!executionInfo.isBatch()) {
                return;
            }
            queryInfoList.stream()
                    .map(QueryInfo::getQuery)
                    .map(this::normalize)
                    .forEach(sql -> executions.add(new BatchExecution(sql, executionInfo.getBatchSize())));
        }

        synchronized void reset() {
            executions.clear();
        }

        synchronized List<Integer> batchSizesFor(String tableName) {
            String insertPrefix = "insert into " + tableName.toLowerCase(Locale.ROOT);
            return executions.stream()
                    .filter(execution -> execution.sql().contains(insertPrefix))
                    .map(BatchExecution::batchSize)
                    .toList();
        }

        private String normalize(String sql) {
            return sql.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
        }
    }

    private record BatchExecution(String sql, int batchSize) {
    }
}
