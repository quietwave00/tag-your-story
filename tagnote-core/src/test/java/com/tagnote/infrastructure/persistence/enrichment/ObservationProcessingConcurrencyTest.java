package com.tagnote.infrastructure.persistence.enrichment;

import com.tagnote.application.enrichment.ObservationProcessingService;
import com.tagnote.application.enrichment.ObservationWriteService;
import com.tagnote.application.enrichment.model.ExternalTagInput;
import com.tagnote.application.enrichment.model.ObservationProcessingResult;
import com.tagnote.domain.catalog.album.AlbumEntity;
import com.tagnote.domain.catalog.track.TrackEntity;
import com.tagnote.domain.enrichment.assertion.EvidenceType;
import com.tagnote.domain.enrichment.observation.ExternalTagSource;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

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
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ObservationProcessingConcurrencyTest {

    @Autowired private ObservationProcessingService processingService;
    @Autowired private ExternalTagObservationJpaRepository observationRepository;
    @Autowired private TagAssertionJpaRepository assertionRepository;
    @Autowired private TagJpaRepository tagRepository;
    @Autowired private TagAliasJpaRepository aliasRepository;
    @Autowired private TrackJpaRepository trackRepository;
    @Autowired private AlbumJpaRepository albumRepository;
    @Autowired private TagNameNormalizer normalizer;

    private Long trackId;

    @BeforeEach
    void setUp() {
        AlbumEntity album = albumRepository.saveAndFlush(AlbumEntity.create("Album", "album-concurrent", 2024));
        TrackEntity track = trackRepository.saveAndFlush(TrackEntity.create(
                "Track", "track-concurrent", "ISRC", 180_000, album
        ));
        trackId = track.getTrackId();
        TagEntity tag = tagRepository.saveAndFlush(TagEntity.create(
                "Ambient", "ambient-concurrent", TagType.GENRE, TagStatus.ACTIVE, null
        ));
        aliasRepository.saveAndFlush(TagAliasEntity.create(
                tag,
                "Ambient",
                normalizer.normalize("Ambient"),
                AliasSource.ADMIN,
                AliasStatus.APPROVED
        ));
    }

    @AfterEach
    void cleanUp() {
        assertionRepository.deleteAllInBatch();
        observationRepository.deleteAllInBatch();
        aliasRepository.deleteAllInBatch();
        tagRepository.deleteAllInBatch();
        trackRepository.deleteAllInBatch();
        albumRepository.deleteAllInBatch();
    }

    @Test
    void 같은_Subject와_입력을_동시에_처리해도_중복_row가_남지_않는다() throws Exception {
        ExternalTagInput input = new ExternalTagInput(
                ExternalTagSource.MUSICBRAINZ,
                "Ambient",
                "recording:concurrent",
                EvidenceType.EXPLICIT_GENRE,
                0.9
        );

        List<ObservationProcessingResult> results = runConcurrently(
                () -> processingService.process(SubjectType.TRACK, trackId, List.of(input)),
                () -> processingService.process(SubjectType.TRACK, trackId, List.of(input))
        );

        assertThat(results).hasSize(2);
        assertThat(observationRepository.count()).isEqualTo(1);
        assertThat(assertionRepository.count()).isEqualTo(1);
    }

    private List<ObservationProcessingResult> runConcurrently(
            Callable<ObservationProcessingResult> firstCall,
            Callable<ObservationProcessingResult> secondCall
    ) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<ObservationProcessingResult> first = executor.submit(() -> callAfterStart(start, firstCall));
            Future<ObservationProcessingResult> second = executor.submit(() -> callAfterStart(start, secondCall));
            start.countDown();
            return List.of(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS));
        } finally {
            executor.shutdownNow();
        }
    }

    private ObservationProcessingResult callAfterStart(
            CountDownLatch start,
            Callable<ObservationProcessingResult> call
    ) throws Exception {
        if (!start.await(5, TimeUnit.SECONDS)) {
            throw new IllegalStateException("Concurrent processing did not start in time");
        }
        return call.call();
    }
}
