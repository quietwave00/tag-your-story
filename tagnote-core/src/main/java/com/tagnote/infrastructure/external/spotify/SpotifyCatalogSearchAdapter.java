package com.tagnote.infrastructure.external.spotify;

import com.tagnote.application.catalog.search.model.CatalogSearchItem;
import com.tagnote.application.catalog.search.model.CatalogSearchResult;
import com.tagnote.application.catalog.search.model.CatalogSearchScope;
import com.tagnote.application.catalog.search.model.CatalogSearchSubjectType;
import com.tagnote.application.catalog.search.model.CatalogSearchWindow;
import com.tagnote.application.catalog.search.port.CatalogSearchProvider;
import com.tagnote.core.domain.tracks.webclient.SpotifyWebClient;
import com.tagnote.core.domain.tracks.webclient.dto.SpotifyCatalogSearchInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import se.michaelthelin.spotify.model_objects.specification.AlbumSimplified;
import se.michaelthelin.spotify.model_objects.specification.Artist;
import se.michaelthelin.spotify.model_objects.specification.ArtistSimplified;
import se.michaelthelin.spotify.model_objects.specification.Image;
import se.michaelthelin.spotify.model_objects.specification.Track;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class SpotifyCatalogSearchAdapter implements CatalogSearchProvider {

    private static final String NO_IMAGE = "NO_IMAGE";

    private final SpotifyWebClient spotifyWebClient;

    @Override
    public CatalogSearchResult search(String keyword, int page, CatalogSearchScope scope) {
        SpotifyCatalogSearchInfo searchInfo = spotifyWebClient.searchCatalog(keyword, page, spotifyTypes(scope));
        List<CatalogSearchItem> items = new ArrayList<>();
        long totalCount = 0;
        if (scope == CatalogSearchScope.ALL || scope == CatalogSearchScope.TRACK) {
            addTracks(items, searchInfo.getTracks(), 0);
            totalCount += searchInfo.getTrackTotalCount();
        }
        if (scope == CatalogSearchScope.ALL || scope == CatalogSearchScope.ALBUM) {
            addAlbums(items, searchInfo.getAlbums(), 0);
            totalCount += searchInfo.getAlbumTotalCount();
        }
        if (scope == CatalogSearchScope.ALL || scope == CatalogSearchScope.ARTIST) {
            addArtists(items, searchInfo.getArtists(), 0);
            totalCount += searchInfo.getArtistTotalCount();
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
            SpotifyCatalogSearchInfo general = spotifyWebClient.searchCatalog(keyword, page, "track,artist");
            SpotifyCatalogSearchInfo albumByTitle = spotifyWebClient.searchCatalog(albumTitleQuery(keyword), page, "album");
            if (index == 0) {
                trackTotal = general.getTrackTotalCount();
                albumTotal = albumByTitle.getAlbumTotalCount();
                artistTotal = general.getArtistTotalCount();
            }
            addTracks(tracks, general.getTracks(), page * 10);
            addAlbums(albums, albumByTitle.getAlbums(), page * 10);
            addArtists(artists, general.getArtists(), page * 10);
        }
        return new CatalogSearchWindow(tracks, albums, artists, trackTotal, albumTotal, artistTotal);
    }

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
            AlbumSimplified album = track.getAlbum();
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

    private void addAlbums(List<CatalogSearchItem> items, AlbumSimplified[] albums, int rankOffset) {
        if (albums == null) return;
        for (int rank = 0; rank < albums.length; rank++) {
            AlbumSimplified album = albums[rank];
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

    private String firstArtistName(ArtistSimplified[] artists) {
        return artists.length == 0 ? null : artists[0].getName();
    }

    private String firstImageUrl(Image[] images) {
        return images.length == 0 ? NO_IMAGE : images[0].getUrl();
    }
}
