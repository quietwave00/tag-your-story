package com.tagnote.application.catalog.detail.model;

public record SpotifyAlbumTrack(String spotifyId, String title, String artistName,
                                Integer discNumber, Integer trackNumber, Integer durationMs) {
}
