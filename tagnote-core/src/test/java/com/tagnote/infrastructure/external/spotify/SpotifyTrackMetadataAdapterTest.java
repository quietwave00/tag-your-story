package com.tagnote.infrastructure.external.spotify;

import com.tagnote.infrastructure.external.spotify.dto.SpotifyJson;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class SpotifyTrackMetadataAdapterTest {
    private final SpotifyHttpClient client = mock(SpotifyHttpClient.class);
    private final SpotifyTrackMetadataAdapter adapter = new SpotifyTrackMetadataAdapter(client);

    @Test
    void import_preserves_both_artist_credit_orders_and_isrc() {
        com.tagnote.infrastructure.external.spotify.dto.SpotifyJson.Track track = SpotifyFixtures.read("""
                {"id":"t1","name":"Title","duration_ms":240000,"external_ids":{"isrc":"ISRC-1"},
                 "artists":[{"id":"a","name":"A"},{"id":"a","name":"Duplicate"},{"id":"b","name":"B"}],
                 "album":{"id":"al","name":"Album","release_date":"2024-03-15","artists":[{"id":"c","name":"C"}]}}
                """, SpotifyJson.Track.class);
        when(client.track("t1")).thenReturn(track);
        com.tagnote.application.catalog.importer.model.SpotifyTrackMetadata metadata = adapter.getTrack("t1");
        assertThat(metadata.getIsrc()).isEqualTo("ISRC-1");
        assertThat(metadata.getReleaseYear()).isEqualTo(2024);
        assertThat(metadata.getTrackArtists()).extracting(a -> a.getSpotifyArtistId() + ":" + a.getPosition())
                .containsExactly("a:0", "b:1");
        assertThat(metadata.getAlbumArtists()).extracting(a -> a.getSpotifyArtistId() + ":" + a.getPosition())
                .containsExactly("c:0");
    }

    @Test
    void missing_track_artists_rejects_import() {
        com.tagnote.infrastructure.external.spotify.dto.SpotifyJson.Track track = SpotifyFixtures.read("""
                {"id":"t1","name":"Title","duration_ms":240000,"artists":[],"album":{"id":"al","name":"Album"}}
                """, SpotifyJson.Track.class);
        when(client.track("t1")).thenReturn(track);
        assertThatThrownBy(() -> adapter.getTrack("t1")).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("track artists");
    }
}
