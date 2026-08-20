package com.tagnote.infrastructure.persistence.catalog;

import com.tagnote.application.catalog.importer.CatalogTrackReadService;
import com.tagnote.application.catalog.importer.CatalogWriteService;
import com.tagnote.application.catalog.importer.model.ImportedTrack;
import com.tagnote.application.catalog.importer.model.SpotifyArtistMetadata;
import com.tagnote.application.catalog.importer.model.SpotifyTrackMetadata;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ContextConfiguration(classes = CatalogJpaTestConfiguration.class)
@Import({CatalogWriteService.class, CatalogTrackReadService.class})
@ActiveProfiles("local")
@TestPropertySource(properties = {
        "spring.sql.init.mode=never",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class CatalogJpaRepositoryTest {

    @Autowired
    private CatalogWriteService catalogWriteService;

    @Autowired
    private CatalogTrackReadService catalogTrackReadService;

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

    @Test
    void 전체_Artist_credit을_순서대로_저장하고_같은_metadata는_멱등하다() {
        SpotifyTrackMetadata metadata = metadata("track-1");

        catalogWriteService.upsert(metadata);
        catalogWriteService.upsert(metadata);

        assertThat(artistRepository.count()).isEqualTo(3);
        assertThat(albumRepository.count()).isEqualTo(1);
        assertThat(trackRepository.count()).isEqualTo(1);
        assertThat(albumArtistRepository.count()).isEqualTo(1);
        assertThat(trackArtistRepository.count()).isEqualTo(2);

        ImportedTrack imported = catalogTrackReadService.getBySpotifyId("track-1");
        assertThat(imported.getArtists())
                .extracting(artist -> artist.getSpotifyArtistId() + ":" + artist.getPosition())
                .containsExactly("artist-a:0", "artist-b:1");
        assertThat(imported.getAlbum().getArtists())
                .extracting(artist -> artist.getSpotifyArtistId() + ":" + artist.getPosition())
                .containsExactly("album-artist:0");
    }

    @Test
    void 서로_다른_Track이_같은_Album과_Artist를_재사용한다() {
        catalogWriteService.upsert(metadata("track-1"));
        catalogWriteService.upsert(metadata("track-2"));

        assertThat(artistRepository.count()).isEqualTo(3);
        assertThat(albumRepository.count()).isEqualTo(1);
        assertThat(trackRepository.count()).isEqualTo(2);
        assertThat(albumArtistRepository.count()).isEqualTo(1);
        assertThat(trackArtistRepository.count()).isEqualTo(4);
    }

    private SpotifyTrackMetadata metadata(String trackId) {
        return SpotifyTrackMetadata.of(
                trackId,
                "title",
                "ISRC",
                240_000,
                List.of(
                        SpotifyArtistMetadata.of("artist-a", "A", 0),
                        SpotifyArtistMetadata.of("artist-b", "B", 1)
                ),
                "album-1",
                "album",
                2024,
                List.of(SpotifyArtistMetadata.of("album-artist", "Album Artist", 0))
        );
    }
}
