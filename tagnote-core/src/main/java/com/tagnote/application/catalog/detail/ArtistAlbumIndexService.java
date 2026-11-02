package com.tagnote.application.catalog.detail;

import com.tagnote.application.catalog.detail.model.ArtistAlbumIndex;
import com.tagnote.application.catalog.detail.model.ArtistAlbumIndexPage;
import com.tagnote.application.catalog.detail.model.CatalogReleaseType;
import com.tagnote.application.catalog.detail.model.SpotifyAlbumSummary;
import com.tagnote.application.catalog.detail.model.SpotifyCatalogPage;
import com.tagnote.application.catalog.detail.port.ArtistAlbumIndexStore;
import com.tagnote.application.catalog.detail.port.SpotifyCatalogDetailProvider;
import com.tagnote.core.exception.CustomException;
import com.tagnote.core.exception.ExceptionCode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ArtistAlbumIndexService {
    private static final int PAGE_SIZE = 10;
    private final SpotifyCatalogDetailProvider provider;
    private final ArtistAlbumIndexStore store;
    private final Executor executor;
    private final ConcurrentHashMap<String, Boolean> building = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, RuntimeException> failures = new ConcurrentHashMap<>();

    public ArtistAlbumIndexService(SpotifyCatalogDetailProvider provider, ArtistAlbumIndexStore store,
                                   @Qualifier("artistAlbumIndexExecutor") Executor executor) {
        this.provider = provider;
        this.store = store;
        this.executor = executor;
    }

    public Optional<ArtistAlbumIndexPage> read(String artistId, String releaseType, int page) {
        ArtistAlbumIndex index = store.find(artistId);
        if (index != null) return Optional.of(slice(index, releaseType, page));
        RuntimeException failure = failures.remove(artistId);
        if (failure != null) throw failure;
        if (building.putIfAbsent(artistId, Boolean.TRUE) == null) {
            boolean scheduled = false;
            try {
                // A previous build can finish between the first cache read and winning this slot.
                ArtistAlbumIndex ready = store.find(artistId);
                if (ready != null) {
                    return Optional.of(slice(ready, releaseType, page));
                }
                RuntimeException completedFailure = failures.remove(artistId);
                if (completedFailure != null) throw completedFailure;
                executor.execute(() -> buildAndPublish(artistId));
                scheduled = true;
            } catch (RuntimeException e) {
                throw new CustomException(ExceptionCode.SPOTIFY_EXCEPTION);
            } finally {
                if (!scheduled) building.remove(artistId);
            }
        }
        return Optional.empty();
    }

    private void buildAndPublish(String artistId) {
        try {
            store.save(artistId, build(artistId));
        } catch (RuntimeException e) {
            log.warn("Artist album index build failed for {}", artistId, e);
            failures.put(artistId, new CustomException(ExceptionCode.SPOTIFY_EXCEPTION));
        } finally {
            building.remove(artistId);
        }
    }

    private ArtistAlbumIndex build(String artistId) {
        List<SpotifyAlbumSummary> all = new ArrayList<>();
        List<Integer> albums = new ArrayList<>();
        List<Integer> singles = new ArrayList<>();
        List<Integer> compilations = new ArrayList<>();
        long total = -1;
        for (int page = 0; ; page++) {
            SpotifyCatalogPage<SpotifyAlbumSummary> fetched = provider.getArtistAlbums(artistId, page);
            if (fetched == null || fetched.items() == null || fetched.totalCount() < 0
                    || fetched.totalCount() > Integer.MAX_VALUE || total >= 0 && fetched.totalCount() != total) {
                throw new CustomException(ExceptionCode.SPOTIFY_EXCEPTION);
            }
            total = fetched.totalCount();
            List<SpotifyAlbumSummary> items = fetched.items();
            long offset = (long) page * PAGE_SIZE;
            int windowCapacity = (int) Math.min(PAGE_SIZE, total - offset);
            if (windowCapacity < 0 || items.size() > windowCapacity) {
                log.warn("Artist album index page exceeds requested window: artistId={}, page={}, total={}, offset={}, capacity={}, actual={}",
                        artistId, page, total, offset, windowCapacity, items.size());
                throw new CustomException(ExceptionCode.SPOTIFY_EXCEPTION);
            }
            for (SpotifyAlbumSummary item : items) {
                if (item == null) throw new CustomException(ExceptionCode.SPOTIFY_EXCEPTION);
                int position = all.size();
                all.add(item);
                CatalogReleaseType type = item.releaseType();
                if (type == CatalogReleaseType.ALBUM) albums.add(position);
                if (type == CatalogReleaseType.SINGLE_EP) singles.add(position);
                if (type == CatalogReleaseType.COMPILATION) compilations.add(position);
            }
            if (offset + PAGE_SIZE >= total) break;
        }
        if (total > 0 && all.isEmpty()) throw new CustomException(ExceptionCode.SPOTIFY_EXCEPTION);
        int unclassified = all.size() - albums.size() - singles.size() - compilations.size();
        return new ArtistAlbumIndex(all, albums, singles, compilations, unclassified == 0,
                unclassified, System.currentTimeMillis(), 1);
    }

    private ArtistAlbumIndexPage slice(ArtistAlbumIndex index, String releaseType, int page) {
        List<SpotifyAlbumSummary> source;
        long total;
        if ("ALL".equals(releaseType)) {
            source = index.allItems();
            total = source.size();
        } else {
            List<Integer> positions = index.positions(releaseType);
            int start = (int) Math.min(positions.size(), (long) page * PAGE_SIZE);
            int end = (int) Math.min(positions.size(), (long) start + PAGE_SIZE);
            source = positions.subList(start, end).stream().map(index.allItems()::get).toList();
            total = positions.size();
        }
        if ("ALL".equals(releaseType)) {
            int start = (int) Math.min(source.size(), (long) page * PAGE_SIZE);
            source = source.subList(start, (int) Math.min(source.size(), (long) start + PAGE_SIZE));
        }
        return new ArtistAlbumIndexPage(source, total, index.classificationComplete(), index.unclassifiedCount());
    }
}
