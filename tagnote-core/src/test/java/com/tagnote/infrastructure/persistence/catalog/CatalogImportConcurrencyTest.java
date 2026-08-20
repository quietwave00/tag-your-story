package com.tagnote.infrastructure.persistence.catalog;

import com.tagnote.application.catalog.importer.CatalogTrackReadService;
import com.tagnote.application.catalog.importer.CatalogWriteService;
import com.tagnote.application.catalog.importer.TrackImportService;
import com.tagnote.application.catalog.importer.model.ImportedTrack;
import com.tagnote.application.catalog.importer.model.SpotifyArtistMetadata;
import com.tagnote.application.catalog.importer.model.SpotifyTrackMetadata;
import com.tagnote.application.catalog.importer.port.SpotifyTrackMetadataProvider;
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

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@DataJpaTest
@ContextConfiguration(classes = CatalogJpaTestConfiguration.class)
@Import({TrackImportService.class, CatalogWriteService.class, CatalogTrackReadService.class})
@ActiveProfiles("local")
@TestPropertySource(properties = {
        "spring.sql.init.mode=never",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class CatalogImportConcurrencyTest {

    @Autowired
    private TrackImportService trackImportService;

    @Autowired
    private ArtistJpaRepository artistRepository;

    @Autowired
    private AlbumJpaRepository albumRepository;

    @Autowired
    private TrackJpaRepository trackRepository;

    @Autowired
    private AlbumArtistJpaRepository albumArtistRepository;

    @Autowired
    private TrackArtistJpaRepository trackArtistRepository;

    @MockBean
    private SpotifyTrackMetadataProvider spotifyTrackMetadataProvider;

    @AfterEach
    void cleanUpCommittedRows() {
        trackArtistRepository.deleteAllInBatch();
        albumArtistRepository.deleteAllInBatch();
        trackRepository.deleteAllInBatch();
        albumRepository.deleteAllInBatch();
        artistRepository.deleteAllInBatch();
    }

    @Test
    void 같은_Track을_동시에_최초_Import해도_하나의_Catalog만_남는다() throws Exception {
        CountDownLatch providerCalls = new CountDownLatch(2);
        when(spotifyTrackMetadataProvider.getTrack("track-1")).thenAnswer(invocation -> {
            awaitBothRequests(providerCalls);
            return metadata("track-1", "album-1");
        });

        List<ImportedTrack> results = runConcurrently(
                () -> trackImportService.importTrack("track-1"),
                () -> trackImportService.importTrack("track-1")
        );

        assertThat(results)
                .extracting(ImportedTrack::getCatalogTrackId)
                .containsOnly(results.get(0).getCatalogTrackId());
        assertThat(artistRepository.count()).isEqualTo(3);
        assertThat(albumRepository.count()).isEqualTo(1);
        assertThat(trackRepository.count()).isEqualTo(1);
        assertThat(albumArtistRepository.count()).isEqualTo(1);
        assertThat(trackArtistRepository.count()).isEqualTo(2);
    }

    @Test
    void 같은_Album과_Artist를_공유하는_서로_다른_Track을_동시에_Import해도_부모_row를_재사용한다() throws Exception {
        CountDownLatch providerCalls = new CountDownLatch(2);
        when(spotifyTrackMetadataProvider.getTrack(anyString())).thenAnswer(invocation -> {
            String trackId = invocation.getArgument(0);
            awaitBothRequests(providerCalls);
            return metadata(trackId, "shared-album");
        });

        List<ImportedTrack> results = runConcurrently(
                () -> trackImportService.importTrack("track-1"),
                () -> trackImportService.importTrack("track-2")
        );

        assertThat(results)
                .extracting(result -> result.getAlbum().getAlbumId())
                .containsOnly(results.get(0).getAlbum().getAlbumId());
        assertThat(results)
                .extracting(ImportedTrack::getSpotifyTrackId)
                .containsExactlyInAnyOrder("track-1", "track-2");
        assertThat(artistRepository.count()).isEqualTo(3);
        assertThat(albumRepository.count()).isEqualTo(1);
        assertThat(trackRepository.count()).isEqualTo(2);
        assertThat(albumArtistRepository.count()).isEqualTo(1);
        assertThat(trackArtistRepository.count()).isEqualTo(4);
    }

    private List<ImportedTrack> runConcurrently(
            Callable<ImportedTrack> firstImport,
            Callable<ImportedTrack> secondImport
    ) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<ImportedTrack> first = executor.submit(() -> {
                awaitStart(start);
                return firstImport.call();
            });
            Future<ImportedTrack> second = executor.submit(() -> {
                awaitStart(start);
                return secondImport.call();
            });

            start.countDown();
            return List.of(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS));
        } finally {
            executor.shutdownNow();
        }
    }

    private void awaitBothRequests(CountDownLatch providerCalls) throws InterruptedException {
        providerCalls.countDown();
        if (!providerCalls.await(5, TimeUnit.SECONDS)) {
            throw new IllegalStateException("Concurrent import requests did not reach the provider in time");
        }
    }

    private void awaitStart(CountDownLatch start) throws InterruptedException {
        if (!start.await(5, TimeUnit.SECONDS)) {
            throw new IllegalStateException("Concurrent import requests did not start in time");
        }
    }

    private SpotifyTrackMetadata metadata(String trackId, String albumId) {
        return SpotifyTrackMetadata.of(
                trackId,
                "title-" + trackId,
                "ISRC-" + trackId,
                240_000,
                List.of(
                        SpotifyArtistMetadata.of("artist-a", "A", 0),
                        SpotifyArtistMetadata.of("artist-b", "B", 1)
                ),
                albumId,
                "album",
                2024,
                List.of(SpotifyArtistMetadata.of("album-artist", "Album Artist", 0))
        );
    }
}
