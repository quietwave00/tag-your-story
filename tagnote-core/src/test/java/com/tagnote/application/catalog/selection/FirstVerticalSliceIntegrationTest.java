package com.tagnote.application.catalog.selection;

import com.tagnote.application.catalog.detail.TrackDetailReadService;
import com.tagnote.application.catalog.detail.model.TrackDetail;
import com.tagnote.application.catalog.importer.CatalogTrackReadService;
import com.tagnote.application.catalog.importer.CatalogWriteService;
import com.tagnote.application.catalog.importer.CatalogExternalIdentityWriteService;
import com.tagnote.application.catalog.importer.TrackImportService;
import com.tagnote.application.catalog.importer.model.SpotifyArtistMetadata;
import com.tagnote.application.catalog.importer.model.SpotifyTrackMetadata;
import com.tagnote.application.catalog.importer.port.SpotifyTrackMetadataProvider;
import com.tagnote.application.enrichment.ExternalEnrichmentCollector;
import com.tagnote.application.enrichment.ObservationProcessingService;
import com.tagnote.application.enrichment.ObservationWriteService;
import com.tagnote.application.enrichment.model.CatalogExternalIdentityMatch;
import com.tagnote.application.enrichment.model.CollectedExternalTags;
import com.tagnote.application.enrichment.model.ExternalEnrichmentCollection;
import com.tagnote.application.enrichment.model.ExternalTagInput;
import com.tagnote.application.resolution.TagInheritanceService;
import com.tagnote.application.resolution.TagResolutionService;
import com.tagnote.application.resolution.TagResolutionWriteService;
import com.tagnote.application.resolution.config.TagResolutionProperties;
import com.tagnote.domain.enrichment.assertion.EvidenceType;
import com.tagnote.domain.enrichment.observation.ExternalTagSource;
import com.tagnote.domain.enrichment.observation.ObservationStatus;
import com.tagnote.domain.resolution.CanonicalTagService;
import com.tagnote.domain.resolution.TagResolver;
import com.tagnote.domain.taxonomy.alias.AliasSource;
import com.tagnote.domain.taxonomy.alias.AliasStatus;
import com.tagnote.domain.taxonomy.alias.TagAliasEntity;
import com.tagnote.domain.taxonomy.matching.TagMatchingService;
import com.tagnote.domain.taxonomy.matching.TagNameNormalizer;
import com.tagnote.domain.taxonomy.tag.TagEntity;
import com.tagnote.domain.taxonomy.tag.TagStatus;
import com.tagnote.domain.taxonomy.tag.TagType;
import com.tagnote.infrastructure.persistence.catalog.AlbumArtistJpaRepository;
import com.tagnote.infrastructure.persistence.catalog.AlbumJpaRepository;
import com.tagnote.infrastructure.persistence.catalog.ArtistJpaRepository;
import com.tagnote.infrastructure.persistence.catalog.TrackArtistJpaRepository;
import com.tagnote.infrastructure.persistence.catalog.TrackJpaRepository;
import com.tagnote.infrastructure.persistence.catalog.HibernateCatalogConflictTranslator;
import com.tagnote.infrastructure.persistence.enrichment.ExternalTagObservationJpaRepository;
import com.tagnote.infrastructure.persistence.enrichment.HibernateEnrichmentConflictTranslator;
import com.tagnote.infrastructure.persistence.enrichment.TagAssertionJpaRepository;
import com.tagnote.infrastructure.persistence.resolution.HibernateResolutionConflictTranslator;
import com.tagnote.infrastructure.persistence.resolution.SubjectTagResolvedJpaRepository;
import com.tagnote.infrastructure.persistence.taxonomy.TagAliasJpaRepository;
import com.tagnote.infrastructure.persistence.taxonomy.TagJpaRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DataJpaTest
@ContextConfiguration(classes = SelectionJpaTestConfiguration.class)
@Import({
        TrackSelectionService.class,
        TrackDetailReadService.class,
        TrackImportService.class,
        CatalogWriteService.class,
        CatalogExternalIdentityWriteService.class,
        CatalogTrackReadService.class,
        HibernateCatalogConflictTranslator.class,
        ObservationProcessingService.class,
        ObservationWriteService.class,
        HibernateEnrichmentConflictTranslator.class,
        TagResolutionService.class,
        TagResolutionWriteService.class,
        TagInheritanceService.class,
        HibernateResolutionConflictTranslator.class,
        TagResolutionProperties.class,
        TagNameNormalizer.class,
        TagMatchingService.class,
        CanonicalTagService.class,
        TagResolver.class
})
@ActiveProfiles("local")
@TestPropertySource(properties = {
        "spring.sql.init.mode=never",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "tag.resolution.minimum-score=0.50",
        "tag.resolution.album-to-track-inheritance-weight=0.85"
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class FirstVerticalSliceIntegrationTest {

    @Autowired private TrackSelectionService selectionService;
    @Autowired private TagJpaRepository tagRepository;
    @Autowired private TagAliasJpaRepository aliasRepository;
    @Autowired private TagNameNormalizer normalizer;
    @Autowired private ExternalTagObservationJpaRepository observationRepository;
    @Autowired private TagAssertionJpaRepository assertionRepository;
    @Autowired private SubjectTagResolvedJpaRepository resolvedRepository;
    @Autowired private TrackArtistJpaRepository trackArtistRepository;
    @Autowired private AlbumArtistJpaRepository albumArtistRepository;
    @Autowired private TrackJpaRepository trackRepository;
    @Autowired private AlbumJpaRepository albumRepository;
    @Autowired private ArtistJpaRepository artistRepository;
    @Autowired private SelectionJpaTestConfiguration.ImportQueryProbe importQueryProbe;

    @MockBean private SpotifyTrackMetadataProvider spotifyProvider;
    @MockBean private ExternalEnrichmentCollector externalEnrichmentCollector;

    @AfterEach
    void cleanUp() {
        resolvedRepository.deleteAllInBatch();
        assertionRepository.deleteAllInBatch();
        observationRepository.deleteAllInBatch();
        aliasRepository.deleteAllInBatch();
        tagRepository.deleteAllInBatch();
        trackArtistRepository.deleteAllInBatch();
        albumArtistRepository.deleteAllInBatch();
        trackRepository.deleteAllInBatch();
        albumRepository.deleteAllInBatch();
        artistRepository.deleteAllInBatch();
    }

    @Test
    void spotify_선택부터_matched_resolved_detail까지_연결하고_반복_선택은_재사용한다() {
        TagEntity tag = approvedAlias("Ambient");
        when(spotifyProvider.getTrack("track-1")).thenReturn(metadata());
        when(externalEnrichmentCollector.collect(org.mockito.ArgumentMatchers.any())).thenAnswer(invocation -> {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
            return fakeEnrichment();
        });

        TrackDetail first = selectionService.select("track-1");
        TrackDetail second = selectionService.select("track-1");

        assertThat(first.systemTags()).singleElement().satisfies(systemTag -> {
            assertThat(systemTag.tagId()).isEqualTo(tag.getTagId());
            assertThat(systemTag.name()).isEqualTo("Ambient");
            assertThat(systemTag.score()).isEqualTo(0.9);
        });
        assertThat(first.track().getMusicBrainzRecordingId()).isEqualTo("recording-1");
        assertThat(first.track().getAlbum().getMusicBrainzReleaseGroupId())
                .isEqualTo("release-group-1");
        assertThat(second.systemTags()).isEqualTo(first.systemTags());
        assertThat(observationRepository.findAll())
                .extracting(observation -> observation.getStatus())
                .containsExactlyInAnyOrder(ObservationStatus.MATCHED, ObservationStatus.NEW);
        assertThat(assertionRepository.count()).isEqualTo(1);
        assertThat(resolvedRepository.count()).isEqualTo(1);
        assertThat(trackRepository.count()).isEqualTo(1);
        assertThat(albumRepository.count()).isEqualTo(1);
        assertThat(artistRepository.count()).isEqualTo(2);
        assertThat(trackRepository.findAll()).singleElement()
                .extracting(track -> track.getMusicbrainzId())
                .isEqualTo("recording-1");
        assertThat(albumRepository.findAll()).singleElement()
                .extracting(album -> album.getMusicbrainzId())
                .isEqualTo("release-group-1");
        verify(spotifyProvider).getTrack("track-1");
        verify(externalEnrichmentCollector).collect(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void 같은_track을_동시에_최초_선택해도_각_projection이_하나로_수렴한다() throws Exception {
        approvedAlias("Ambient");
        CountDownLatch spotifyCalls = new CountDownLatch(2);
        CountDownLatch externalCalls = new CountDownLatch(2);
        when(spotifyProvider.getTrack("track-1")).thenAnswer(invocation -> {
            awaitBoth(spotifyCalls);
            return metadata();
        });
        when(externalEnrichmentCollector.collect(org.mockito.ArgumentMatchers.any())).thenAnswer(invocation -> {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
            awaitBoth(externalCalls);
            return fakeEnrichment();
        });

        List<TrackDetail> results = runConcurrently(
                () -> selectionService.select("track-1"),
                () -> selectionService.select("track-1")
        );

        assertThat(results).allSatisfy(result -> assertThat(result.systemTags()).hasSize(1));
        assertThat(trackRepository.count()).isEqualTo(1);
        assertThat(observationRepository.count()).isEqualTo(2);
        assertThat(assertionRepository.count()).isEqualTo(1);
        assertThat(resolvedRepository.count()).isEqualTo(1);
        assertThat(trackRepository.findAll()).singleElement()
                .extracting(track -> track.getMusicbrainzId())
                .isEqualTo("recording-1");
        assertThat(albumRepository.findAll()).singleElement()
                .extracting(album -> album.getMusicbrainzId())
                .isEqualTo("release-group-1");
        verify(spotifyProvider, times(2)).getTrack("track-1");
        verify(externalEnrichmentCollector, times(2)).collect(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void first_load는_중간_결과를_재사용해_subject와_최종_detail을_재조회하지_않는다() {
        approvedAlias("Ambient");
        when(spotifyProvider.getTrack("track-1")).thenReturn(metadata());
        when(externalEnrichmentCollector.collect(org.mockito.ArgumentMatchers.any()))
                .thenReturn(fakeThirtyOneTagEnrichment());
        importQueryProbe.reset();

        TrackDetail detail = selectionService.select("track-1");
        int importExecutionCount = importQueryProbe.executionCount();

        assertThat(detail.systemTags()).isEmpty();
        assertThat(observationRepository.count()).isEqualTo(31L);
        assertThat(importQueryProbe.selectCountContaining(" from track_artist ")).isZero();
        assertThat(importQueryProbe.selectCountContaining(" from album_artist ")).isZero();
        assertThat(importQueryProbe.selectCountContaining(" from tag_assertion "))
                .isLessThanOrEqualTo(4L);
        assertThat(importExecutionCount).isLessThanOrEqualTo(32);
    }

    private TagEntity approvedAlias(String name) {
        TagEntity tag = tagRepository.saveAndFlush(TagEntity.create(
                name, "ambient-selection", TagType.GENRE, TagStatus.ACTIVE, null
        ));
        aliasRepository.saveAndFlush(TagAliasEntity.create(
                tag,
                name,
                normalizer.normalize(name),
                AliasSource.ADMIN,
                AliasStatus.APPROVED
        ));
        return tag;
    }

    private SpotifyTrackMetadata metadata() {
        return SpotifyTrackMetadata.of(
                "track-1",
                "Track",
                "ISRC-1",
                180_000,
                List.of(SpotifyArtistMetadata.of("track-artist", "Track Artist", 0)),
                "album-1",
                "Album",
                2026,
                List.of(SpotifyArtistMetadata.of("album-artist", "Album Artist", 0))
        );
    }

    private CollectedExternalTags fakeTags() {
        return new CollectedExternalTags(List.of(), List.of(
                new ExternalTagInput(
                        ExternalTagSource.MUSICBRAINZ,
                        "Ambient",
                        "recording:track-1:genre:ambient",
                        EvidenceType.EXPLICIT_GENRE,
                        0.9
                ),
                new ExternalTagInput(
                        ExternalTagSource.MUSICBRAINZ,
                        "Unmatched Fixture",
                        "recording:track-1:genre:unmatched",
                        EvidenceType.EXPLICIT_GENRE,
                        0.8
                )
        ));
    }

    private ExternalEnrichmentCollection fakeEnrichment() {
        return new ExternalEnrichmentCollection(
                fakeTags(),
                new CatalogExternalIdentityMatch("recording-1", "release-group-1"),
                List.of()
        );
    }

    private ExternalEnrichmentCollection fakeThirtyOneTagEnrichment() {
        List<ExternalTagInput> albumInputs = new ArrayList<>();
        List<ExternalTagInput> trackInputs = new ArrayList<>();
        for (int index = 1; index <= 12; index++) {
            albumInputs.add(new ExternalTagInput(
                    ExternalTagSource.MUSICBRAINZ,
                    "Unmatched Album Fixture " + index,
                    "release:album-1:genre:unmatched:" + index,
                    EvidenceType.EXPLICIT_GENRE,
                    0.8
            ));
        }
        for (int index = 1; index <= 19; index++) {
            trackInputs.add(new ExternalTagInput(
                    ExternalTagSource.MUSICBRAINZ,
                    "Unmatched Track Fixture " + index,
                    "recording:track-1:genre:unmatched:" + index,
                    EvidenceType.EXPLICIT_GENRE,
                    0.8
            ));
        }
        return new ExternalEnrichmentCollection(
                new CollectedExternalTags(albumInputs, trackInputs),
                new CatalogExternalIdentityMatch("recording-1", "release-group-1"),
                List.of()
        );
    }

    private List<TrackDetail> runConcurrently(
            Callable<TrackDetail> firstCall,
            Callable<TrackDetail> secondCall
    ) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<TrackDetail> first = executor.submit(() -> callAfterStart(start, firstCall));
            Future<TrackDetail> second = executor.submit(() -> callAfterStart(start, secondCall));
            start.countDown();
            return List.of(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS));
        } finally {
            executor.shutdownNow();
        }
    }

    private TrackDetail callAfterStart(CountDownLatch start, Callable<TrackDetail> call) throws Exception {
        if (!start.await(5, TimeUnit.SECONDS)) {
            throw new IllegalStateException("Concurrent selection did not start in time");
        }
        return call.call();
    }

    private void awaitBoth(CountDownLatch calls) throws InterruptedException {
        calls.countDown();
        if (!calls.await(5, TimeUnit.SECONDS)) {
            throw new IllegalStateException("Concurrent calls did not arrive in time");
        }
    }
}
