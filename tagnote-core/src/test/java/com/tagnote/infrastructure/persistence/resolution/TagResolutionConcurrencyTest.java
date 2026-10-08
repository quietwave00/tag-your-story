package com.tagnote.infrastructure.persistence.resolution;

import com.tagnote.application.resolution.TagInheritanceService;
import com.tagnote.application.resolution.TagResolutionService;
import com.tagnote.application.resolution.TagResolutionWriteService;
import com.tagnote.application.resolution.config.TagResolutionProperties;
import com.tagnote.application.resolution.model.ResolvedTagResult;
import com.tagnote.domain.catalog.album.AlbumEntity;
import com.tagnote.domain.catalog.track.TrackEntity;
import com.tagnote.domain.enrichment.assertion.AssertionSource;
import com.tagnote.domain.enrichment.assertion.EvidenceType;
import com.tagnote.domain.enrichment.assertion.TagAssertionEntity;
import com.tagnote.domain.enrichment.subject.SubjectRef;
import com.tagnote.domain.resolution.CanonicalTagService;
import com.tagnote.domain.resolution.TagResolver;
import com.tagnote.domain.taxonomy.tag.TagEntity;
import com.tagnote.domain.taxonomy.tag.TagStatus;
import com.tagnote.domain.taxonomy.tag.TagType;
import com.tagnote.infrastructure.persistence.catalog.AlbumJpaRepository;
import com.tagnote.infrastructure.persistence.catalog.TrackJpaRepository;
import com.tagnote.infrastructure.persistence.enrichment.TagAssertionJpaRepository;
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
@ContextConfiguration(classes = ResolutionJpaTestConfiguration.class)
@Import({
        TagResolutionService.class,
        TagResolutionWriteService.class,
        TagInheritanceService.class,
        HibernateResolutionConflictTranslator.class,
        CanonicalTagService.class,
        TagResolver.class,
        TagResolutionProperties.class
})
@ActiveProfiles("local")
@TestPropertySource(properties = {
        "spring.sql.init.mode=never",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "tag.resolution.minimum-score=0.50",
        "tag.resolution.album-to-track-inheritance-weight=0.85"
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class TagResolutionConcurrencyTest {

    @Autowired private TagResolutionService resolutionService;
    @Autowired private SubjectTagResolvedJpaRepository resolvedRepository;
    @Autowired private TagAssertionJpaRepository assertionRepository;
    @Autowired private TagJpaRepository tagRepository;
    @Autowired private TrackJpaRepository trackRepository;
    @Autowired private AlbumJpaRepository albumRepository;

    private Long trackId;

    @BeforeEach
    void setUp() {
        AlbumEntity album = albumRepository.saveAndFlush(AlbumEntity.create(
                "Concurrent Album", "resolution-concurrent-album", 2026
        ));
        TrackEntity track = trackRepository.saveAndFlush(TrackEntity.create(
                "Concurrent Track", "resolution-concurrent-track", "ISRC-C", 180_000, album
        ));
        trackId = track.getTrackId();
        TagEntity tag = tagRepository.saveAndFlush(TagEntity.create(
                "Ambient", "resolution-concurrent-ambient", TagType.GENRE, TagStatus.ACTIVE, null
        ));
        assertionRepository.saveAndFlush(TagAssertionEntity.createApproved(
                SubjectRef.track(track),
                tag,
                AssertionSource.MUSICBRAINZ,
                EvidenceType.EXPLICIT_GENRE,
                0.9
        ));
    }

    @AfterEach
    void cleanUp() {
        resolvedRepository.deleteAllInBatch();
        assertionRepository.deleteAllInBatch();
        tagRepository.deleteAllInBatch();
        trackRepository.deleteAllInBatch();
        albumRepository.deleteAllInBatch();
    }

    @Test
    void 같은_Subject를_동시에_resolve해도_unique_재시도후_한_row만_남는다() throws Exception {
        List<List<ResolvedTagResult>> results = runConcurrently(
                () -> resolutionService.resolve(SubjectRef.track(trackId)),
                () -> resolutionService.resolve(SubjectRef.track(trackId))
        );

        assertThat(results).allSatisfy(result -> assertThat(result).hasSize(1));
        assertThat(resolvedRepository.count()).isEqualTo(1);
    }

    private List<List<ResolvedTagResult>> runConcurrently(
            Callable<List<ResolvedTagResult>> firstCall,
            Callable<List<ResolvedTagResult>> secondCall
    ) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<List<ResolvedTagResult>> first = executor.submit(() -> callAfterStart(start, firstCall));
            Future<List<ResolvedTagResult>> second = executor.submit(() -> callAfterStart(start, secondCall));
            start.countDown();
            return List.of(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS));
        } finally {
            executor.shutdownNow();
        }
    }

    private List<ResolvedTagResult> callAfterStart(
            CountDownLatch start,
            Callable<List<ResolvedTagResult>> call
    ) throws Exception {
        if (!start.await(5, TimeUnit.SECONDS)) {
            throw new IllegalStateException("Concurrent resolution did not start in time");
        }
        return call.call();
    }
}
