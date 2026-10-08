package com.tagnote.infrastructure.external.spotify;

import com.tagnote.application.catalog.search.model.CatalogSearchItem;
import com.tagnote.application.catalog.search.model.CatalogSearchResult;
import com.tagnote.application.catalog.search.model.CatalogSearchSubjectType;
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
    public CatalogSearchResult search(String keyword, int page) {
        SpotifyCatalogSearchInfo searchInfo = spotifyWebClient.searchCatalog(keyword, page);
        List<CatalogSearchItem> items = new ArrayList<>();
        addTracks(items, searchInfo.getTracks());
        addAlbums(items, searchInfo.getAlbums());
        addArtists(items, searchInfo.getArtists());

        long totalCount = (long) searchInfo.getTrackTotalCount()
                + searchInfo.getAlbumTotalCount()
                + searchInfo.getArtistTotalCount();
        return CatalogSearchResult.of(items, totalCount);
    }

    private void addTracks(List<CatalogSearchItem> items, Track[] tracks) {
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
                    rank
            ));
        }
    }

    private void addAlbums(List<CatalogSearchItem> items, AlbumSimplified[] albums) {
        for (int rank = 0; rank < albums.length; rank++) {
            AlbumSimplified album = albums[rank];
            items.add(CatalogSearchItem.of(
                    CatalogSearchSubjectType.ALBUM,
                    album.getId(),
                    album.getName(),
                    firstArtistName(album.getArtists()),
                    null,
                    firstImageUrl(album.getImages()),
                    rank
            ));
        }
    }

    private void addArtists(List<CatalogSearchItem> items, Artist[] artists) {
        for (int rank = 0; rank < artists.length; rank++) {
            Artist artist = artists[rank];
            items.add(CatalogSearchItem.of(
                    CatalogSearchSubjectType.ARTIST,
                    artist.getId(),
                    artist.getName(),
                    null,
                    null,
                    firstImageUrl(artist.getImages()),
                    rank
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
