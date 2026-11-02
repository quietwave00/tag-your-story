package com.tagnote.application.catalog.detail;

import com.tagnote.application.catalog.detail.model.CatalogReleaseType;
import com.tagnote.application.catalog.detail.model.ArtistAlbumIndexPage;
import com.tagnote.application.catalog.detail.model.SpotifyAlbumSummary;
import com.tagnote.application.catalog.detail.model.SpotifyCatalogPage;
import com.tagnote.application.catalog.detail.port.ArtistAlbumIndexStore;
import com.tagnote.application.catalog.detail.port.SpotifyCatalogDetailProvider;
import com.tagnote.core.exception.CustomException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ArtistAlbumIndexServiceTest {
    private final SpotifyCatalogDetailProvider provider = mock(SpotifyCatalogDetailProvider.class);
    private final ArtistAlbumIndexStore store = mock(ArtistAlbumIndexStore.class);
    private final List<Runnable> tasks = new ArrayList<>();
    private final Executor executor = tasks::add;
    private final ArtistAlbumIndexService service = new ArtistAlbumIndexService(provider, store, executor);

    @Test
    void 동시_첫_요청은_한_번만_구축하고_유형별_페이지와_원본_순서를_제공한다() {
        AtomicReference<com.tagnote.application.catalog.detail.model.ArtistAlbumIndex> cached = new AtomicReference<>();
        when(store.find("artist")).thenAnswer(invocation -> cached.get());
        doAnswer(invocation -> { cached.set(invocation.getArgument(1)); return null; })
                .when(store).save(eq("artist"), any());
        List<SpotifyAlbumSummary> releases = new ArrayList<>();
        releases.add(item("comp", CatalogReleaseType.COMPILATION));
        releases.add(item("single-1", CatalogReleaseType.SINGLE_EP));
        releases.add(item("single-2", CatalogReleaseType.SINGLE_EP));
        for (int i = 1; i <= 11; i++) releases.add(item("album-" + i, CatalogReleaseType.ALBUM));
        when(provider.getArtistAlbums("artist", 0)).thenReturn(new SpotifyCatalogPage<>(releases.subList(0, 10), 14));
        when(provider.getArtistAlbums("artist", 1)).thenReturn(new SpotifyCatalogPage<>(releases.subList(10, 14), 14));

        assertThat(service.read("artist", "ALL", 0)).isEmpty();
        assertThat(service.read("artist", "ALBUM", 0)).isEmpty();
        assertThat(tasks).hasSize(1);
        tasks.get(0).run();

        com.tagnote.application.catalog.detail.model.ArtistAlbumIndexPage first = service.read("artist", "ALBUM", 0).orElseThrow();
        com.tagnote.application.catalog.detail.model.ArtistAlbumIndexPage second = service.read("artist", "ALBUM", 1).orElseThrow();
        assertThat(first.items()).extracting(SpotifyAlbumSummary::spotifyId)
                .containsExactlyElementsOf(releases.subList(3, 13).stream().map(SpotifyAlbumSummary::spotifyId).toList());
        assertThat(second.items()).extracting(SpotifyAlbumSummary::spotifyId).containsExactly("album-11");
        assertThat(first.totalCount()).isEqualTo(11);
        assertThat(first.classificationComplete()).isTrue();
        assertThat(service.read("artist", "ALL", 1).orElseThrow().items())
                .extracting(SpotifyAlbumSummary::spotifyId)
                .containsExactlyElementsOf(releases.subList(10, 14).stream().map(SpotifyAlbumSummary::spotifyId).toList());
        assertThat(service.read("artist", "SINGLE_EP", 0).orElseThrow().totalCount()).isEqualTo(2);
        ArtistAlbumIndexPage compilations = service.read("artist", "COMPILATION", 0).orElseThrow();
        assertThat(compilations.totalCount()).isEqualTo(1);
        assertThat(compilations.items()).extracting(SpotifyAlbumSummary::spotifyId).containsExactly("comp");
        assertThat(compilations.items()).extracting(SpotifyAlbumSummary::releaseType)
                .containsExactly(CatalogReleaseType.COMPILATION);
        assertThat(service.read("artist", "ALBUM", 2).orElseThrow().items()).isEmpty();
        verify(provider).getArtistAlbums("artist", 0);
        verify(provider).getArtistAlbums("artist", 1);
        verifyNoMoreInteractions(provider);
    }

    @Test
    void 미분류_항목은_ALL에_남기고_유형별_건수에서_제외한다() {
        AtomicReference<com.tagnote.application.catalog.detail.model.ArtistAlbumIndex> cached = new AtomicReference<>();
        when(store.find("artist")).thenAnswer(invocation -> cached.get());
        doAnswer(invocation -> { cached.set(invocation.getArgument(1)); return null; })
                .when(store).save(eq("artist"), any());
        when(provider.getArtistAlbums("artist", 0)).thenReturn(new SpotifyCatalogPage<>(
                List.of(item("unknown", null), item("album", CatalogReleaseType.ALBUM)), 2));

        service.read("artist", "ALL", 0);
        tasks.get(0).run();

        assertThat(service.read("artist", "ALL", 0).orElseThrow().totalCount()).isEqualTo(2);
        com.tagnote.application.catalog.detail.model.ArtistAlbumIndexPage albums = service.read("artist", "ALBUM", 0).orElseThrow();
        assertThat(albums.totalCount()).isEqualTo(1);
        assertThat(albums.classificationComplete()).isFalse();
        assertThat(albums.unclassifiedCount()).isEqualTo(1);
    }

    @Test
    void 짧은_원본_페이지_뒤의_offset도_조회하고_실제로_받은_항목만_집계한다() {
        AtomicReference<com.tagnote.application.catalog.detail.model.ArtistAlbumIndex> cached = new AtomicReference<>();
        when(store.find("artist")).thenAnswer(invocation -> cached.get());
        doAnswer(invocation -> { cached.set(invocation.getArgument(1)); return null; })
                .when(store).save(eq("artist"), any());
        List<SpotifyAlbumSummary> first = new ArrayList<>();
        for (int index = 0; index < 10; index++) {
            first.add(item("album-" + index, CatalogReleaseType.ALBUM));
        }
        List<SpotifyAlbumSummary> second = List.of(
                item("comp-1", CatalogReleaseType.COMPILATION),
                item("comp-2", CatalogReleaseType.COMPILATION),
                item("single-1", CatalogReleaseType.SINGLE_EP),
                item("single-2", CatalogReleaseType.SINGLE_EP),
                item("single-3", CatalogReleaseType.SINGLE_EP));
        List<SpotifyAlbumSummary> third = List.of(item("album-10", CatalogReleaseType.ALBUM));
        when(provider.getArtistAlbums("artist", 0)).thenReturn(new SpotifyCatalogPage<>(first, 21));
        when(provider.getArtistAlbums("artist", 1)).thenReturn(new SpotifyCatalogPage<>(second, 21));
        when(provider.getArtistAlbums("artist", 2)).thenReturn(new SpotifyCatalogPage<>(third, 21));

        assertThat(service.read("artist", "ALL", 0)).isEmpty();
        tasks.get(0).run();

        assertThat(service.read("artist", "ALL", 0).orElseThrow().totalCount()).isEqualTo(16);
        assertThat(service.read("artist", "ALL", 1).orElseThrow().items())
                .extracting(SpotifyAlbumSummary::spotifyId)
                .containsExactly("comp-1", "comp-2", "single-1", "single-2", "single-3", "album-10");
        ArtistAlbumIndexPage compilations = service.read("artist", "COMPILATION", 0).orElseThrow();
        assertThat(compilations.totalCount()).isEqualTo(2);
        assertThat(compilations.items()).extracting(SpotifyAlbumSummary::spotifyId)
                .containsExactly("comp-1", "comp-2");
        verify(provider).getArtistAlbums("artist", 2);
    }

    @Test
    void 동일한_원본_창의_항목도_중복을_보존한다() {
        AtomicReference<com.tagnote.application.catalog.detail.model.ArtistAlbumIndex> cached = new AtomicReference<>();
        when(store.find("artist")).thenAnswer(invocation -> cached.get());
        doAnswer(invocation -> { cached.set(invocation.getArgument(1)); return null; })
                .when(store).save(eq("artist"), any());
        List<SpotifyAlbumSummary> page = new ArrayList<>();
        for (int index = 0; index < 10; index++) page.add(item("same-" + index, CatalogReleaseType.ALBUM));
        when(provider.getArtistAlbums("artist", 0)).thenReturn(new SpotifyCatalogPage<>(page, 20));
        when(provider.getArtistAlbums("artist", 1)).thenReturn(new SpotifyCatalogPage<>(page, 20));

        service.read("artist", "ALBUM", 0);
        tasks.get(0).run();

        assertThat(service.read("artist", "ALBUM", 1).orElseThrow().items())
                .extracting(SpotifyAlbumSummary::spotifyId)
                .containsExactlyElementsOf(page.stream().map(SpotifyAlbumSummary::spotifyId).toList());
        assertThat(service.read("artist", "ALBUM", 1).orElseThrow().totalCount()).isEqualTo(20);
    }

    @Test
    void 누락된_후속_원본_페이지는_게시하지_않고_오류_후_재시도한다() {
        when(provider.getArtistAlbums("artist", 0)).thenReturn(new SpotifyCatalogPage<>(List.of(item("a", null)), 11));

        assertThat(service.read("artist", "ALL", 0)).isEmpty();
        tasks.get(0).run();

        verify(store, never()).save(eq("artist"), any());
        assertThatThrownBy(() -> service.read("artist", "ALL", 0)).isInstanceOf(CustomException.class);
        assertThat(service.read("artist", "ALL", 0)).isEmpty();
        assertThat(tasks).hasSize(2);
    }

    private SpotifyAlbumSummary item(String id, CatalogReleaseType type) {
        return new SpotifyAlbumSummary(id, id, "NO_IMAGE", null, type);
    }
}
