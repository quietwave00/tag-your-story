package com.tagnote.application.catalog.search;

import com.tagnote.application.catalog.search.model.CatalogScreenSearchResult;
import com.tagnote.application.catalog.search.model.CatalogSearchItem;
import com.tagnote.application.catalog.search.model.CatalogSearchSubjectType;
import com.tagnote.application.catalog.search.model.CatalogSearchWindow;
import com.tagnote.application.catalog.search.port.CatalogSearchProvider;
import com.tagnote.application.catalog.search.port.CatalogSearchWindowStore;
import com.tagnote.application.catalog.search.port.SearchKeywordRecorder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CatalogScreenSearchServiceTest {
    @Mock private SearchKeywordRecorder recorder;
    @Mock private CatalogSearchProvider provider;
    @Mock private CatalogSearchWindowStore store;

    @Test
    void 세번째_Spotify_페이지의_정확한_곡을_첫_화면_페이지로_올리고_같은_window를_재사용한다() {
        List<CatalogSearchItem> tracks = new ArrayList<>();
        for (int index = 0; index < 29; index++) {
            tracks.add(item(CatalogSearchSubjectType.TRACK, "track-" + index, "Other " + index, index));
        }
        tracks.add(item(CatalogSearchSubjectType.TRACK, "exact", "Queen", 29));
        CatalogSearchWindow fetched = new CatalogSearchWindow(tracks,
                List.of(item(CatalogSearchSubjectType.ALBUM, "album", "Album", 0)),
                List.of(item(CatalogSearchSubjectType.ARTIST, "artist", "Artist", 0)), 200, 50, 7);
        AtomicReference<CatalogSearchWindow> cached = new AtomicReference<>();
        when(store.find("Queen", 0)).thenAnswer(invocation -> cached.get());
        when(provider.searchWindow("Queen", 0, 3)).thenReturn(fetched);
        org.mockito.Mockito.doAnswer(invocation -> {
            cached.set(invocation.getArgument(2));
            return null;
        }).when(store).save(org.mockito.ArgumentMatchers.eq("Queen"), org.mockito.ArgumentMatchers.eq(0),
                org.mockito.ArgumentMatchers.any(CatalogSearchWindow.class));
        CatalogScreenSearchService service = new CatalogScreenSearchService(recorder, provider, store);

        CatalogScreenSearchResult first = service.search("Queen", 0, 0, 0);
        CatalogScreenSearchResult third = service.search("Queen", 2, 0, 0);

        assertThat(first.tracks().items()).hasSize(10);
        assertThat(first.tracks().items().get(0).getSpotifyId()).isEqualTo("exact");
        assertThat(first.tracks().totalCount()).isEqualTo(200);
        assertThat(first.tracks().hasNext()).isTrue();
        assertThat(third.tracks().items()).hasSize(10);
        assertThat(third.tracks().items()).extracting(CatalogSearchItem::getSpotifyId).doesNotContain("exact");
        assertThat(third.tracks().hasNext()).isTrue();
        assertThat(first.albums().items()).hasSize(1);
        assertThat(first.artists().items()).hasSize(1);
        verify(provider).searchWindow("Queen", 0, 3);
        verifyNoMoreInteractions(provider);
    }

    @Test
    void 검색_최대_페이지에서는_Spotify_offset_1000까지만_조회하고_다음_페이지를_열지_않는다() {
        List<CatalogSearchItem> tracks = new ArrayList<>();
        for (int index = 0; index < 20; index++) {
            tracks.add(item(CatalogSearchSubjectType.TRACK, "track-" + index, "Song " + index, index));
        }
        when(provider.searchWindow("song", 99, 2)).thenReturn(new CatalogSearchWindow(
                tracks, List.of(), List.of(), 10000, 0, 0));
        when(provider.searchWindow("song", 0, 3)).thenReturn(new CatalogSearchWindow(
                List.of(), List.of(), List.of(), 10000, 0, 0));
        CatalogScreenSearchService service = new CatalogScreenSearchService(recorder, provider, store);

        CatalogScreenSearchResult result = service.search("song", 100, 100, 100);

        assertThat(result.tracks().items()).hasSize(10);
        assertThat(result.tracks().hasNext()).isFalse();
        assertThat(result.albums().items()).isEmpty();
        assertThat(result.albums().hasNext()).isFalse();
        verify(provider).searchWindow("song", 99, 2);
        verify(provider).searchWindow("song", 0, 3);
    }

    @Test
    void 타입별_페이지가_다른_window에_있으면_필요한_window만_각각_조회한다() {
        when(provider.searchWindow("Queen", 0, 3)).thenReturn(new CatalogSearchWindow(
                List.of(), List.of(item(CatalogSearchSubjectType.ALBUM, "album-0", "Album", 0)),
                List.of(item(CatalogSearchSubjectType.ARTIST, "artist-0", "Queen", 0)), 0, 1, 1));
        when(provider.searchWindow("Queen", 3, 3)).thenReturn(new CatalogSearchWindow(
                List.of(item(CatalogSearchSubjectType.TRACK, "track-3", "Song", 30)),
                List.of(), List.of(), 1, 0, 0));
        CatalogScreenSearchService service = new CatalogScreenSearchService(recorder, provider, store);

        CatalogScreenSearchResult result = service.search("Queen", 3, 0, 0);

        assertThat(result.tracks().items()).extracting(CatalogSearchItem::getSpotifyId)
                .containsExactly("track-3");
        assertThat(result.albums().items()).extracting(CatalogSearchItem::getSpotifyId)
                .containsExactly("album-0");
        assertThat(result.artists().items()).extracting(CatalogSearchItem::getSpotifyId)
                .containsExactly("artist-0");
        verify(provider).searchWindow("Queen", 0, 3);
        verify(provider).searchWindow("Queen", 3, 3);
    }

    @Test
    void 전체_건수가_커도_빈_window에서는_다음_페이지를_닫는다() {
        when(provider.searchWindow("Queen", 3, 3)).thenReturn(new CatalogSearchWindow(
                List.of(), List.of(), List.of(), 10000, 10000, 10000));
        when(provider.searchWindow("Queen", 0, 3)).thenReturn(new CatalogSearchWindow(
                List.of(), List.of(), List.of(), 10000, 10000, 10000));
        CatalogScreenSearchService service = new CatalogScreenSearchService(recorder, provider, store);

        CatalogScreenSearchResult result = service.search("Queen", 3, 0, 0);

        assertThat(result.tracks().items()).isEmpty();
        assertThat(result.tracks().hasNext()).isFalse();
        assertThat(result.tracks().totalCount()).isEqualTo(10000);
    }

    @Test
    void 기존_타입별_검색도_열한번째_정확_일치를_첫_페이지로_올리고_동일_ID_중복을_제거한다() {
        List<CatalogSearchItem> tracks = new ArrayList<>();
        for (int index = 0; index < 10; index++) {
            tracks.add(item(CatalogSearchSubjectType.TRACK, "other-" + index,
                    "In Love With " + index, index));
        }
        tracks.add(item(CatalogSearchSubjectType.TRACK, "exact", "In Love", 10));
        tracks.add(item(CatalogSearchSubjectType.TRACK, "other-0", "In Love With 0", 11));
        when(provider.searchWindow("in love", 0, 3)).thenReturn(new CatalogSearchWindow(
                tracks, List.of(), List.of(), 60, 0, 0));
        CatalogScreenSearchService service = new CatalogScreenSearchService(recorder, provider, store);

        var result = service.searchType("in love", 0,
                com.tagnote.application.catalog.search.model.CatalogSearchScope.TRACK);
        var second = service.searchType("in love", 1,
                com.tagnote.application.catalog.search.model.CatalogSearchScope.TRACK);

        assertThat(result.getItems().get(0).getSpotifyId()).isEqualTo("exact");
        assertThat(result.getItems()).extracting(CatalogSearchItem::getSpotifyId)
                .doesNotHaveDuplicates();
        assertThat(second.getItems()).hasSize(1);
        assertThat(result.getItems()).extracting(CatalogSearchItem::getSpotifyId)
                .doesNotContain(second.getItems().get(0).getSpotifyId());
        assertThat(result.getTotalCount()).isEqualTo(60);
    }

    private CatalogSearchItem item(CatalogSearchSubjectType type, String id, String title, int rank) {
        return CatalogSearchItem.of(type, id, title, null, null, "NO_IMAGE", rank);
    }
}
