package com.tagnote.infrastructure.external.spotify;

import com.tagnote.application.catalog.search.model.CatalogSearchItem;
import com.tagnote.application.catalog.search.model.CatalogSearchResult;
import com.tagnote.application.catalog.search.model.CatalogSearchScope;
import com.tagnote.application.catalog.search.model.CatalogSearchSubjectType;
import com.tagnote.application.catalog.search.model.CatalogSearchWindow;
import com.tagnote.application.catalog.search.port.CatalogSearchProvider;
import com.tagnote.infrastructure.external.spotify.dto.SpotifyJson.Album;
import com.tagnote.infrastructure.external.spotify.dto.SpotifyJson.Artist;
import com.tagnote.infrastructure.external.spotify.dto.SpotifyJson.Image;
import com.tagnote.infrastructure.external.spotify.dto.SpotifyJson.Page;
import com.tagnote.infrastructure.external.spotify.dto.SpotifyJson.Track;
import com.tagnote.infrastructure.external.spotify.dto.SpotifyJson;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SpotifyCatalogSearchAdapter implements CatalogSearchProvider {

    private static final String NO_IMAGE = "NO_IMAGE";

    private final SpotifyHttpClient client;

    @Override
    public CatalogSearchResult search(String keyword, int page, CatalogSearchScope scope) {
        SpotifyJson.Search searchInfo = client.search(keyword, page, spotifyTypes(scope));
        List<CatalogSearchItem> items = new ArrayList<>();
        long totalCount = 0;
        if (scope == CatalogSearchScope.ALL || scope == CatalogSearchScope.TRACK) {
            addTracks(items, items(searchInfo.getTracks()), 0);
            totalCount += total(searchInfo.getTracks());
        }
        if (scope == CatalogSearchScope.ALL || scope == CatalogSearchScope.ALBUM) {
            addAlbums(items, items(searchInfo.getAlbums()), 0);
            totalCount += total(searchInfo.getAlbums());
        }
        if (scope == CatalogSearchScope.ALL || scope == CatalogSearchScope.ARTIST) {
            addArtists(items, items(searchInfo.getArtists()), 0);
            totalCount += total(searchInfo.getArtists());
        }
        return CatalogSearchResult.of(items, totalCount);
    }

    @Override
    public CatalogSearchWindow searchWindow(String keyword, int firstPage, int pageCount) {
        List<CatalogSearchItem> tracks = new ArrayList<>();
        List<CatalogSearchItem> albums = new ArrayList<>();
        List<CatalogSearchItem> artists = new ArrayList<>();
        long trackTotal = 0;
        long albumTotal = 0;
        long artistTotal = 0;
        for (int index = 0; index < pageCount; index++) {
            int page = firstPage + index;
            SpotifyJson.Search general = client.search(keyword, page, "track,artist");
            SpotifyJson.Search albumByTitle = client.search(albumTitleQuery(keyword), page, "album");
            if (index == 0) {
                trackTotal = total(general.getTracks());
                albumTotal = total(albumByTitle.getAlbums());
                artistTotal = total(general.getArtists());
            }
            addTracks(tracks, items(general.getTracks()), page * 10);
            addAlbums(albums, items(albumByTitle.getAlbums()), page * 10);
            addArtists(artists, items(general.getArtists()), page * 10);
        }
        return new CatalogSearchWindow(tracks, albums, artists, trackTotal, albumTotal, artistTotal);
    }

    private static <T> T[] items(Page<T> page) { return page == null ? null : page.getItems(); }

    private static int total(Page<?> page) { return page == null || page.getTotal() == null ? 0 : page.getTotal(); }

    private String albumTitleQuery(String keyword) {
        return "album:" + keyword.trim();
    }

    private String spotifyTypes(CatalogSearchScope scope) {
        return switch (scope) {
            case ALL -> "track,album,artist";
            case TRACK -> "track";
            case ALBUM -> "album";
            case ARTIST -> "artist";
        };
    }

    private void addTracks(List<CatalogSearchItem> items, Track[] tracks, int rankOffset) {
        if (tracks == null) return;
        for (int rank = 0; rank < tracks.length; rank++) {
            Track track = tracks[rank];
            Album album = track.getAlbum();
            items.add(CatalogSearchItem.of(
                    CatalogSearchSubjectType.TRACK,
                    track.getId(),
                    track.getName(),
                    firstArtistName(track.getArtists()),
                    album.getName(),
                    firstImageUrl(album.getImages()),
                    rankOffset + rank
            ));
        }
    }

    private void addAlbums(List<CatalogSearchItem> items, Album[] albums, int rankOffset) {
        if (albums == null) return;
        for (int rank = 0; rank < albums.length; rank++) {
            Album album = albums[rank];
            items.add(CatalogSearchItem.of(
                    CatalogSearchSubjectType.ALBUM,
                    album.getId(),
                    album.getName(),
                    firstArtistName(album.getArtists()),
                    null,
                    firstImageUrl(album.getImages()),
                    rankOffset + rank
            ));
        }
    }

    private void addArtists(List<CatalogSearchItem> items, Artist[] artists, int rankOffset) {
        if (artists == null) return;
        for (int rank = 0; rank < artists.length; rank++) {
            Artist artist = artists[rank];
            items.add(CatalogSearchItem.of(
                    CatalogSearchSubjectType.ARTIST,
                    artist.getId(),
                    artist.getName(),
                    null,
                    null,
                    firstImageUrl(artist.getImages()),
                    rankOffset + rank
            ));
        }
    }

    private String firstArtistName(Artist[] artists) {
        return artists.length == 0 ? null : artists[0].getName();
    }

    private String firstImageUrl(Image[] images) {
        return images.length == 0 ? NO_IMAGE : images[0].getUrl();
    }
}
