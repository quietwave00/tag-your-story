package com.tagnote.infrastructure.external.spotify;

import com.tagnote.application.catalog.search.model.CatalogSearchItem;
import com.tagnote.application.catalog.search.model.CatalogSearchScope;
import com.tagnote.infrastructure.external.spotify.dto.SpotifyJson;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class SpotifyCatalogSearchAdapterTest {
    private final SpotifyHttpClient client = mock(SpotifyHttpClient.class);
    private final SpotifyCatalogSearchAdapter adapter = new SpotifyCatalogSearchAdapter(client);

    @Test
    void combined_search_preserves_type_order_fields_and_totals() {
        com.tagnote.infrastructure.external.spotify.dto.SpotifyJson.Search result = SpotifyFixtures.read("""
                {"tracks":{"total":10,"items":[{"id":"t","name":"Track","artists":[{"name":"Singer"}],
                "album":{"name":"Record","images":[{"url":"track-cover"}]}}]},
                "albums":{"total":20,"items":[{"id":"al","name":"Album","artists":[{"name":"Band"}],"images":[]}]},
                "artists":{"total":30,"items":[{"id":"ar","name":"Artist","images":[{"url":"portrait"}]}]}}
                """, SpotifyJson.Search.class);
        when(client.search("queen", 1, "track,album,artist")).thenReturn(result);
        com.tagnote.application.catalog.search.model.CatalogSearchResult mapped = adapter.search("queen", 1, CatalogSearchScope.ALL);
        assertThat(mapped.getItems()).extracting(CatalogSearchItem::getSpotifyId).containsExactly("t", "al", "ar");
        assertThat(mapped.getTotalCount()).isEqualTo(60);
        assertThat(mapped.getItems().get(0).getArtistName()).isEqualTo("Singer");
        assertThat(mapped.getItems().get(1).getImageUrl()).isEqualTo("NO_IMAGE");
        verify(client).search("queen", 1, "track,album,artist");
    }

    @Test
    void screen_window_uses_title_filtered_album_query_and_original_ranks() {
        com.tagnote.infrastructure.external.spotify.dto.SpotifyJson.Search first = SpotifyFixtures.read("""
                {"tracks":{"total":100,"items":[{"id":"t1","name":"First","artists":[],"album":{"name":"A","images":[]}}]},
                "artists":{"total":5,"items":[]}}
                """, SpotifyJson.Search.class);
        com.tagnote.infrastructure.external.spotify.dto.SpotifyJson.Search second = SpotifyFixtures.read("""
                {"tracks":{"total":101,"items":[{"id":"t2","name":"Second","artists":[],"album":{"name":"A","images":[]}}]},
                "artists":{"total":5,"items":[]}}
                """, SpotifyJson.Search.class);
        com.tagnote.infrastructure.external.spotify.dto.SpotifyJson.Search albums = SpotifyFixtures.read("""
                {"albums":{"total":20,"items":[{"id":"al","name":"Album","artists":[],"images":[]}]}}
                """, SpotifyJson.Search.class);
        when(client.search("Exact", 0, "track,artist")).thenReturn(first);
        when(client.search("Exact", 1, "track,artist")).thenReturn(second);
        when(client.search("album:Exact", 0, "album")).thenReturn(albums);
        when(client.search("album:Exact", 1, "album")).thenReturn(albums);
        com.tagnote.application.catalog.search.model.CatalogSearchWindow window = adapter.searchWindow("Exact", 0, 2);
        assertThat(window.tracks()).extracting(CatalogSearchItem::getSpotifyId).containsExactly("t1", "t2");
        assertThat(window.tracks()).extracting(CatalogSearchItem::getProviderRank).containsExactly(0, 10);
        assertThat(window.trackTotalCount()).isEqualTo(100);
        assertThat(window.albumTotalCount()).isEqualTo(20);
        verify(client).search("album:Exact", 0, "album");
        verify(client).search("album:Exact", 1, "album");
    }
}
