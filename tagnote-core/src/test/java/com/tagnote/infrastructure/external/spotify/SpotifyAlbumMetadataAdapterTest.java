package com.tagnote.infrastructure.external.spotify;

import com.tagnote.infrastructure.external.spotify.dto.SpotifyJson;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class SpotifyAlbumMetadataAdapterTest {
    @Test
    void album_import_preserves_unique_artist_credit_order_and_year() {
        var client = mock(SpotifyHttpClient.class);
        var album = SpotifyFixtures.read("""
                {"id":"al","name":"Album","release_date":"2024-03-15",
                "artists":[{"id":"a","name":"A"},{"id":"a","name":"Other"},{"id":"b","name":"B"}]}
                """, SpotifyJson.Album.class);
        when(client.album("al")).thenReturn(album);
        var result = new SpotifyAlbumMetadataAdapter(client).getAlbum("al");
        assertThat(result.spotifyId()).isEqualTo("al");
        assertThat(result.releaseYear()).isEqualTo(2024);
        assertThat(result.artists()).extracting(a -> a.getSpotifyArtistId() + ":" + a.getPosition())
                .containsExactly("a:0", "b:1");
    }
}
