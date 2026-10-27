package com.tagnote.application.catalog.detail.model;

import java.util.List;

public record SpotifyArtistDetail(String spotifyId, String name, String imageUrl, List<String> genres) {
    public SpotifyArtistDetail {
        genres = List.copyOf(genres);
    }
}
