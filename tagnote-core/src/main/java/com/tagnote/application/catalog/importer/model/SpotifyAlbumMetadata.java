package com.tagnote.application.catalog.importer.model;

import java.util.List;

public record SpotifyAlbumMetadata(String spotifyId, String title, Integer releaseYear,
                                   List<SpotifyArtistMetadata> artists) {
    public SpotifyAlbumMetadata {
        artists = List.copyOf(artists);
    }
}
