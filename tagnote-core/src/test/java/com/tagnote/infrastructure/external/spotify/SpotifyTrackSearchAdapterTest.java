package com.tagnote.infrastructure.external.spotify;

import com.tagnote.application.catalog.search.model.TrackSearchItem;
import com.tagnote.infrastructure.external.spotify.dto.SpotifyJson;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class SpotifyTrackSearchAdapterTest {
    private final SpotifyHttpClient client = mock(SpotifyHttpClient.class);
    private final SpotifyTrackSearchAdapter adapter = new SpotifyTrackSearchAdapter(client);

    @Test
    void search_preserves_order_total_and_fallback_image() {
        SpotifyJson.Search response = SpotifyFixtures.read("""
                {"tracks":{"total":25,"items":[
                  {"id":"t1","name":"First","artists":[{"name":"A"}],"album":{"name":"Album","images":[{"url":"cover"}]}},
                  {"id":"t2","name":"Second","artists":[{"name":"B"}],"album":{"name":"Other","images":[]}}
                ]}}
                """, SpotifyJson.Search.class);
        when(client.searchTracks("rock", 2)).thenReturn(response.getTracks());

        com.tagnote.application.catalog.search.model.TrackSearchResult result = adapter.search("rock", 2);

        assertThat(result.getItems()).extracting(TrackSearchItem::getSpotifyTrackId).containsExactly("t1", "t2");
        assertThat(result.getTotalCount()).isEqualTo(25);
        assertThat(result.getItems().get(0).getImageUrl()).isEqualTo("cover");
        assertThat(result.getItems().get(1).getImageUrl()).isEqualTo("NO_IMAGE");
        verify(client).searchTracks("rock", 2);
    }
}
