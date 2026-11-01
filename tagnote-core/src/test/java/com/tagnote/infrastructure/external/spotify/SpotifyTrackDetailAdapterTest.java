package com.tagnote.infrastructure.external.spotify;

import com.tagnote.infrastructure.external.spotify.dto.SpotifyJson;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class SpotifyTrackDetailAdapterTest {
    @Test
    void track_detail_uses_first_artist_and_same_response_album_ids() {
        var client = mock(SpotifyHttpClient.class);
        var track = SpotifyFixtures.read("""
                {"id":"t","name":"Title","artists":[{"id":"a","name":"A"},{"id":"b","name":"B"}],
                "album":{"id":"al","name":"Album","images":[]}}
                """, SpotifyJson.Track.class);
        when(client.track("t")).thenReturn(track);
        var result = new SpotifyTrackDetailAdapter(client).getTrack("t");
        assertThat(result.getSpotifyArtistId()).isEqualTo("a");
        assertThat(result.getSpotifyAlbumId()).isEqualTo("al");
        assertThat(result.getImageUrl()).isEqualTo("NO_IMAGE");
        verify(client).track("t");
    }
}
