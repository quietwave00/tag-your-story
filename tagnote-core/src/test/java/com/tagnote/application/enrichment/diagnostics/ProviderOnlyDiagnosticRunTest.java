package com.tagnote.application.enrichment.diagnostics;

import com.tagnote.TestConfiguration;
import com.tagnote.application.catalog.detail.CatalogDetailService;
import com.tagnote.application.catalog.importer.model.ImportedAlbum;
import com.tagnote.application.catalog.importer.model.ImportedArtist;
import com.tagnote.application.catalog.importer.model.ImportedTrack;
import com.tagnote.application.catalog.importer.model.SpotifyArtistMetadata;
import com.tagnote.application.catalog.importer.model.SpotifyTrackMetadata;
import com.tagnote.application.catalog.importer.model.SpotifyAlbumMetadata;
import com.tagnote.application.catalog.importer.port.SpotifyAlbumMetadataProvider;
import com.tagnote.application.catalog.importer.port.SpotifyTrackMetadataProvider;
import com.tagnote.application.enrichment.ExternalEnrichmentCollector;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

/** Run explicitly with DIAGNOSTIC_RUN=true and a DIAGNOSTIC_SPOTIFY_*_ID environment variable. */
@ContextConfiguration(classes = TestConfiguration.class)
@SpringBootTest
@ActiveProfiles("local")
@TestPropertySource(properties = "tag.enrichment.diagnostics.enabled=true")
@EnabledIfEnvironmentVariable(named = "DIAGNOSTIC_RUN", matches = "true")
class ProviderOnlyDiagnosticRunTest {
    private final SpotifyTrackMetadataProvider tracks;
    private final SpotifyAlbumMetadataProvider albums;
    private final CatalogDetailService details;
    private final ExternalEnrichmentCollector collector;
    @MockBean private RedissonClient redissonClient;

    @Autowired
    ProviderOnlyDiagnosticRunTest(SpotifyTrackMetadataProvider tracks, SpotifyAlbumMetadataProvider albums,
                              CatalogDetailService details, ExternalEnrichmentCollector collector) {
        this.tracks = tracks;
        this.albums = albums;
        this.details = details;
        this.collector = collector;
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "DIAGNOSTIC_SPOTIFY_TRACK_ID", matches = ".+")
    void 트랙을_제공자만_다시_수집한다() {
        SpotifyTrackMetadata spotify = tracks.getTrack(System.getenv("DIAGNOSTIC_SPOTIFY_TRACK_ID"));
        ImportedAlbum album = ImportedAlbum.of(0L, spotify.getSpotifyAlbumId(), spotify.getAlbumTitle(),
                spotify.getReleaseYear(), artists(spotify.getAlbumArtists()));
        ImportedTrack track = ImportedTrack.of(0L, spotify.getSpotifyTrackId(), null, spotify.getTitle(),
                spotify.getIsrc(), spotify.getDurationMs(), artists(spotify.getTrackArtists()), album);
        try (EnrichmentDiagnostics.Scope ignored = EnrichmentDiagnostics.open("PROVIDER_ONLY", "TRACK")) {
            EnrichmentDiagnostics.started(track);
            EnrichmentDiagnostics.release(album, details);
            collector.collect(track);
        }
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "DIAGNOSTIC_SPOTIFY_ALBUM_ID", matches = ".+")
    void 앨범을_제공자만_다시_수집한다() {
        SpotifyAlbumMetadata spotify = albums.getAlbum(System.getenv("DIAGNOSTIC_SPOTIFY_ALBUM_ID"));
        ImportedAlbum album = ImportedAlbum.of(0L, spotify.spotifyId(), spotify.title(),
                spotify.releaseYear(), artists(spotify.artists()));
        try (EnrichmentDiagnostics.Scope ignored = EnrichmentDiagnostics.open("PROVIDER_ONLY", "ALBUM")) {
            EnrichmentDiagnostics.started(album);
            EnrichmentDiagnostics.release(album, details);
            collector.collectAlbum(album);
        }
    }

    private List<ImportedArtist> artists(List<SpotifyArtistMetadata> spotify) {
        return spotify.stream().map(artist -> ImportedArtist.of(0L, artist.getSpotifyArtistId(),
                artist.getName(), artist.getPosition())).toList();
    }
}
