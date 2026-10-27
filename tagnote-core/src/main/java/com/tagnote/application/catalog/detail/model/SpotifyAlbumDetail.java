package com.tagnote.application.catalog.detail.model;

import java.util.List;

public record SpotifyAlbumDetail(String spotifyId, String title, String imageUrl,
                                 String releaseDate, Integer totalTracks, List<ArtistCredit> artists) {
    public SpotifyAlbumDetail {
        artists = List.copyOf(artists);
    }

    public record ArtistCredit(String spotifyId, String name) {
    }
}
