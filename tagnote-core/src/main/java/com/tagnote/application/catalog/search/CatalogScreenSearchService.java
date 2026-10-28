package com.tagnote.application.catalog.search;

import com.tagnote.application.catalog.search.model.CatalogScreenSearchResult;
import com.tagnote.application.catalog.search.model.CatalogSearchItem;
import com.tagnote.application.catalog.search.model.CatalogSearchResult;
import com.tagnote.application.catalog.search.model.CatalogSearchScope;
import com.tagnote.application.catalog.search.model.CatalogSearchWindow;
import com.tagnote.application.catalog.search.port.CatalogSearchProvider;
import com.tagnote.application.catalog.search.port.CatalogSearchWindowStore;
import com.tagnote.application.catalog.search.port.SearchKeywordRecorder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.ToIntFunction;

@Service
@RequiredArgsConstructor
public class CatalogScreenSearchService {
    public static final int PAGE_SIZE = 10;
    public static final int WINDOW_PAGES = 3;
    public static final int MAX_PAGE = 100;

    private final SearchKeywordRecorder keywordRecorder;
    private final CatalogSearchProvider provider;
    private final CatalogSearchWindowStore windowStore;
    private final Map<WindowKey, CatalogSearchWindow> inFlight = new ConcurrentHashMap<>();

    public CatalogScreenSearchResult search(String keyword, int trackPage, int albumPage, int artistPage) {
        keywordRecorder.record(keyword);
        Map<Integer, CatalogSearchWindow> windows = new HashMap<>();
        CatalogSearchWindow reference = windows.computeIfAbsent(0, index -> loadWindow(keyword, index));
        CatalogScreenSearchResult.Section tracks = section(keyword, trackPage, windows, CatalogSearchWindow::tracks,
                reference.trackTotalCount(), CatalogSearchWindow::fetchedTrackCount);
        CatalogScreenSearchResult.Section albums = section(keyword, albumPage, windows, CatalogSearchWindow::albums,
                reference.albumTotalCount(), CatalogSearchWindow::fetchedAlbumCount);
        CatalogScreenSearchResult.Section artists = section(keyword, artistPage, windows, CatalogSearchWindow::artists,
                reference.artistTotalCount(), CatalogSearchWindow::fetchedArtistCount);
        return new CatalogScreenSearchResult(tracks, albums, artists);
    }

    public CatalogSearchResult searchType(String keyword, int page, CatalogSearchScope scope) {
        keywordRecorder.record(keyword);
        Map<Integer, CatalogSearchWindow> windows = new HashMap<>();
        CatalogSearchWindow reference = windows.computeIfAbsent(0, index -> loadWindow(keyword, index));
        CatalogScreenSearchResult.Section section = switch (scope) {
            case TRACK -> section(keyword, page, windows, CatalogSearchWindow::tracks,
                    reference.trackTotalCount(), CatalogSearchWindow::fetchedTrackCount);
            case ALBUM -> section(keyword, page, windows, CatalogSearchWindow::albums,
                    reference.albumTotalCount(), CatalogSearchWindow::fetchedAlbumCount);
            case ARTIST -> section(keyword, page, windows, CatalogSearchWindow::artists,
                    reference.artistTotalCount(), CatalogSearchWindow::fetchedArtistCount);
            case ALL -> throw new IllegalArgumentException("searchType requires one subject type");
        };
        return CatalogSearchResult.of(section.items(), section.totalCount());
    }

    private CatalogScreenSearchResult.Section section(
            String keyword,
            int page,
            Map<Integer, CatalogSearchWindow> windows,
            Function<CatalogSearchWindow, List<CatalogSearchItem>> itemsOf,
            long totalCount,
            ToIntFunction<CatalogSearchWindow> fetchedCountOf) {
        int windowIndex = page / WINDOW_PAGES;
        CatalogSearchWindow window = windows.computeIfAbsent(windowIndex, index -> loadWindow(keyword, index));
        List<CatalogSearchItem> candidates = itemsOf.apply(window);
        int start = (page % WINDOW_PAGES) * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, candidates.size());
        List<CatalogSearchItem> items = start >= candidates.size() ? List.of() : candidates.subList(start, end);
        int firstPage = windowIndex * WINDOW_PAGES;
        int pageCount = Math.min(WINDOW_PAGES, MAX_PAGE - firstPage + 1);
        boolean hasNext = candidates.size() > end
                || (fetchedCountOf.applyAsInt(window) == pageCount * PAGE_SIZE
                    && page == firstPage + pageCount - 1 && page < MAX_PAGE);
        return new CatalogScreenSearchResult.Section(items, totalCount, hasNext);
    }

    private CatalogSearchWindow loadWindow(String keyword, int index) {
        WindowKey key = new WindowKey(keyword, index);
        CatalogSearchWindow window = inFlight.computeIfAbsent(key, ignored -> fetchWindow(keyword, index));
        inFlight.remove(key, window);
        return window;
    }

    private CatalogSearchWindow fetchWindow(String keyword, int index) {
        CatalogSearchWindow cached = windowStore.find(keyword, index);
        if (cached != null) return cached;
        int firstPage = index * WINDOW_PAGES;
        int pageCount = Math.min(WINDOW_PAGES, MAX_PAGE - firstPage + 1);
        CatalogSearchWindow fetched = provider.searchWindow(keyword, firstPage, pageCount);
        CatalogSearchWindow ranked = new CatalogSearchWindow(
                CatalogSearchRanker.rank(keyword, unique(fetched.tracks())),
                CatalogSearchRanker.rank(keyword, unique(fetched.albums())),
                CatalogSearchRanker.rank(keyword, unique(fetched.artists())),
                fetched.trackTotalCount(), fetched.albumTotalCount(), fetched.artistTotalCount(),
                fetched.fetchedTrackCount(), fetched.fetchedAlbumCount(), fetched.fetchedArtistCount());
        windowStore.save(keyword, index, ranked);
        return ranked;
    }

    private List<CatalogSearchItem> unique(List<CatalogSearchItem> candidates) {
        Map<String, CatalogSearchItem> bySpotifyId = new LinkedHashMap<>();
        for (CatalogSearchItem item : candidates) {
            bySpotifyId.putIfAbsent(item.getSpotifyId(), item);
        }
        return List.copyOf(bySpotifyId.values());
    }

    private record WindowKey(String keyword, int index) {}
}
