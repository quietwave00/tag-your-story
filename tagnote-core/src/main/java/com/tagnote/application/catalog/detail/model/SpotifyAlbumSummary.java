package com.tagnote.application.catalog.detail.model;

public record SpotifyAlbumSummary(String spotifyId, String title, String imageUrl, String releaseDate,
                                  CatalogReleaseType releaseType) {
}
