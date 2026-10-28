package com.tagnote.application.catalog.search.model;

import java.util.List;

public record CatalogSearchWindow(
        List<CatalogSearchItem> tracks,
        List<CatalogSearchItem> albums,
        List<CatalogSearchItem> artists,
        long trackTotalCount,
        long albumTotalCount,
        long artistTotalCount,
        int fetchedTrackCount,
        int fetchedAlbumCount,
        int fetchedArtistCount
) {
    public CatalogSearchWindow(List<CatalogSearchItem> tracks, List<CatalogSearchItem> albums,
                               List<CatalogSearchItem> artists, long trackTotalCount,
                               long albumTotalCount, long artistTotalCount) {
        this(tracks, albums, artists, trackTotalCount, albumTotalCount, artistTotalCount,
                tracks.size(), albums.size(), artists.size());
    }

    public CatalogSearchWindow {
        tracks = List.copyOf(tracks);
        albums = List.copyOf(albums);
        artists = List.copyOf(artists);
    }
}
