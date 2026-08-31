package com.tagnote.infrastructure.persistence.enrichment;

import com.tagnote.application.enrichment.ObservationProcessingService;
import com.tagnote.application.enrichment.ObservationWriteService;
import com.tagnote.application.enrichment.model.ExternalTagInput;
import com.tagnote.application.enrichment.model.ObservationProcessingResult;
import com.tagnote.domain.catalog.album.AlbumEntity;
import com.tagnote.domain.catalog.track.TrackEntity;
import com.tagnote.domain.enrichment.assertion.AssertionStatus;
import com.tagnote.domain.enrichment.assertion.EvidenceType;
import com.tagnote.domain.enrichment.observation.ExternalTagSource;
import com.tagnote.domain.enrichment.observation.ObservationStatus;
import com.tagnote.domain.enrichment.subject.SubjectType;
import com.tagnote.domain.taxonomy.alias.AliasSource;
import com.tagnote.domain.taxonomy.alias.AliasStatus;
import com.tagnote.domain.taxonomy.alias.TagAliasEntity;
import com.tagnote.domain.taxonomy.matching.TagMatchingService;
import com.tagnote.domain.taxonomy.matching.TagNameNormalizer;
import com.tagnote.domain.taxonomy.tag.TagEntity;
import com.tagnote.domain.taxonomy.tag.TagStatus;
import com.tagnote.domain.taxonomy.tag.TagType;
import com.tagnote.infrastructure.persistence.catalog.AlbumJpaRepository;
import com.tagnote.infrastructure.persistence.catalog.TrackJpaRepository;
import com.tagnote.infrastructure.persistence.taxonomy.TagAliasJpaRepository;
import com.tagnote.infrastructure.persistence.taxonomy.TagJpaRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.PersistenceUnitUtil;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ContextConfiguration(classes = EnrichmentJpaTestConfiguration.class)
@Import({
        ObservationProcessingService.class,
        ObservationWriteService.class,
        HibernateEnrichmentConflictTranslator.class,
        TagNameNormalizer.class,
        TagMatchingService.class
})
@ActiveProfiles("local")
@TestPropertySource(properties = {
        "spring.sql.init.mode=never",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.generate_statistics=true"
})
class EnrichmentJpaRepositoryTest {

    @Autowired
    private ObservationProcessingService processingService;

    @Autowired
    private ExternalTagObservationJpaRepository observationRepository;

    @Autowired
    private TagAssertionJpaRepository assertionRepository;

    @Autowired
    private TagJpaRepository tagRepository;

    @Autowired
    private TagAliasJpaRepository aliasRepository;

    @Autowired
    private AlbumJpaRepository albumRepository;

    @Autowired
    private TrackJpaRepository trackRepository;

    @Autowired
    private TagNameNormalizer normalizer;

    @Autowired
    private EntityManager entityManager;

    private TrackEntity track;

    @BeforeEach
    void setUpSubject() {
        AlbumEntity album = albumRepository.saveAndFlush(AlbumEntity.create("Album", "album-1", 2024));
        track = trackRepository.saveAndFlush(TrackEntity.create("Track", "track-1", "ISRC-1", 180_000, album));
    }

    @Test
    void 열개_raw_tag중_일곱개만_match되어도_전체_Observation과_일곱_Assertion을_보존한다() {
        for (int index = 1; index <= 7; index++) {
            approveAlias("genre-" + index);
        }
        approveAmbiguousAlias("ambiguous");

        List<ExternalTagInput> inputs = new ArrayList<>();
        for (int index = 1; index <= 7; index++) {
            inputs.add(input(ExternalTagSource.MUSICBRAINZ, "Genre-" + index, "tag:" + index));
        }
        inputs.add(input(ExternalTagSource.MUSICBRAINZ, "Unknown A", "tag:8"));
        inputs.add(input(ExternalTagSource.MUSICBRAINZ, "Unknown B", "tag:9"));
        inputs.add(input(ExternalTagSource.MUSICBRAINZ, "Ambiguous", "tag:10"));

        ObservationProcessingResult result = processingService.process(
                SubjectType.TRACK,
                track.getTrackId(),
                inputs
        );

        assertThat(result).isEqualTo(new ObservationProcessingResult(10, 0, 7, 3, 7, 0));
        assertThat(observationRepository.count()).isEqualTo(10);
        assertThat(observationRepository.findAll())
                .filteredOn(observation -> observation.getStatus() == ObservationStatus.NEW)
                .extracting(observation -> observation.getMatchedTag())
                .containsOnlyNulls();
        assertThat(assertionRepository.findAll()).hasSize(7)
                .allMatch(assertion -> assertion.getStatus() == AssertionStatus.APPROVED);
    }

    @Test
    void 같은_입력을_반복하면_Observation과_Assertion을_재사용한다() {
        approveAlias("ambient");
        List<ExternalTagInput> inputs = List.of(input(
                ExternalTagSource.MUSICBRAINZ,
                "  AMBIENT  ",
                "recording:1"
        ));

        ObservationProcessingResult first = processingService.process(
                SubjectType.TRACK, track.getTrackId(), inputs
        );
        entityManager.clear();
        ObservationProcessingResult second = processingService.process(
                SubjectType.TRACK, track.getTrackId(), inputs
        );

        assertThat(first).isEqualTo(new ObservationProcessingResult(1, 0, 1, 0, 1, 0));
        assertThat(second).isEqualTo(new ObservationProcessingResult(0, 1, 1, 0, 0, 1));
        assertThat(observationRepository.count()).isEqualTo(1);
        assertThat(assertionRepository.count()).isEqualTo(1);
        assertThat(observationRepository.findAll().get(0).getRawName()).isEqualTo("  AMBIENT  ");
    }

    @Test
    void 존재하지_않는_Subject는_저장_전에_거부한다() {
        assertThatThrownBy(() -> processingService.process(
                SubjectType.ALBUM,
                999_999L,
                List.of(input(ExternalTagSource.DISCOGS, "Ambient", "release:1"))
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Album subject does not exist");

        assertThat(observationRepository.count()).isZero();
        assertThat(assertionRepository.count()).isZero();
    }

    @Test
    void Provider별_호출은_독립적으로_성공한_raw_tag를_보존한다() {
        approveAlias("ambient");

        processingService.process(SubjectType.TRACK, track.getTrackId(), List.of(
                input(ExternalTagSource.MUSICBRAINZ, "Ambient", "recording:1")
        ));
        processingService.process(SubjectType.TRACK, track.getTrackId(), List.of(
                input(ExternalTagSource.DISCOGS, "Ambient", "release:1")
        ));

        assertThat(observationRepository.findAll())
                .extracting(observation -> observation.getSource())
                .containsExactlyInAnyOrder(ExternalTagSource.MUSICBRAINZ, ExternalTagSource.DISCOGS);
        assertThat(assertionRepository.count()).isEqualTo(2);
    }

    @Test
    void bulk_재처리_query수는_입력_크기에_비례하지_않는다() {
        List<ExternalTagInput> inputs = new ArrayList<>();
        for (int index = 1; index <= 10; index++) {
            approveAlias("genre-" + index);
            inputs.add(input(ExternalTagSource.MUSICBRAINZ, "Genre-" + index, "tag:" + index));
        }
        processingService.process(SubjectType.TRACK, track.getTrackId(), inputs);
        entityManager.clear();

        Statistics statistics = entityManager.getEntityManagerFactory()
                .unwrap(SessionFactory.class)
                .getStatistics();
        statistics.clear();

        processingService.process(SubjectType.TRACK, track.getTrackId(), inputs);

        assertThat(statistics.getPrepareStatementCount()).isLessThanOrEqualTo(4L);
    }

    @Test
    void Observation과_Assertion의_unique와_FK를_DB가_보호한다() {
        TagEntity tag = approveAlias("ambient");
        processingService.process(SubjectType.TRACK, track.getTrackId(), List.of(
                input(ExternalTagSource.MUSICBRAINZ, "Ambient", "recording:1")
        ));
        entityManager.flush();

        assertThatThrownBy(() -> entityManager.createNativeQuery("""
                insert into external_tag_observation
                    (subject_type, subject_id, source, raw_name, normalized_name, external_ref, status, observed_at)
                values ('TRACK', :subjectId, 'MUSICBRAINZ', 'Ambient', 'ambient', 'recording:1', 'NEW', current_timestamp)
                """).setParameter("subjectId", track.getTrackId()).executeUpdate())
                .isInstanceOf(PersistenceException.class);

        entityManager.clear();
        assertThatThrownBy(() -> entityManager.createNativeQuery("""
                insert into tag_assertion
                    (subject_type, subject_id, tag_id, source, evidence_type, confidence, status, created_at)
                values ('TRACK', :subjectId, 999999, 'DISCOGS', 'EXPLICIT_STYLE', 0.8, 'APPROVED', current_timestamp)
                """).setParameter("subjectId", track.getTrackId()).executeUpdate())
                .isInstanceOf(PersistenceException.class);

        assertThat(tag.getTagId()).isNotNull();
    }

    @Test
    void 연관관계는_LAZY이고_bulk_query는_Tag를_함께_적재한다() {
        approveAlias("ambient");
        processingService.process(SubjectType.TRACK, track.getTrackId(), List.of(
                input(ExternalTagSource.MUSICBRAINZ, "Ambient", "recording:1")
        ));
        entityManager.clear();

        PersistenceUnitUtil persistence = entityManager.getEntityManagerFactory().getPersistenceUnitUtil();
        var observation = observationRepository.findAll().get(0);
        var assertion = assertionRepository.findAll().get(0);

        assertThat(persistence.isLoaded(observation, "matchedTag")).isFalse();
        assertThat(persistence.isLoaded(assertion, "tag")).isFalse();

        entityManager.clear();
        var bulkObservations = observationRepository.findExistingForInputs(
                SubjectType.TRACK,
                track.getTrackId(),
                List.of(ExternalTagSource.MUSICBRAINZ),
                List.of("ambient"),
                List.of("recording:1")
        );
        assertThat(persistence.isLoaded(bulkObservations.get(0), "matchedTag")).isTrue();
    }

    private TagEntity approveAlias(String normalizedName) {
        TagEntity tag = tagRepository.saveAndFlush(TagEntity.create(
                normalizedName,
                normalizedName,
                TagType.GENRE,
                TagStatus.ACTIVE,
                null
        ));
        aliasRepository.saveAndFlush(TagAliasEntity.create(
                tag,
                normalizedName,
                normalizer.normalize(normalizedName),
                AliasSource.ADMIN,
                AliasStatus.APPROVED
        ));
        return tag;
    }

    private void approveAmbiguousAlias(String normalizedName) {
        TagEntity first = approveAlias(normalizedName + "-first");
        TagEntity second = approveAlias(normalizedName + "-second");
        aliasRepository.saveAllAndFlush(List.of(
                TagAliasEntity.create(first, normalizedName, normalizer.normalize(normalizedName), AliasSource.ADMIN, AliasStatus.APPROVED),
                TagAliasEntity.create(second, normalizedName, normalizer.normalize(normalizedName), AliasSource.ADMIN, AliasStatus.APPROVED)
        ));
    }

    private ExternalTagInput input(ExternalTagSource source, String rawName, String externalRef) {
        return new ExternalTagInput(source, rawName, externalRef, EvidenceType.EXPLICIT_GENRE, 0.9);
    }
}
