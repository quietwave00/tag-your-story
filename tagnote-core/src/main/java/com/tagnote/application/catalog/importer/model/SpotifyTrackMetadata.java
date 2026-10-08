package com.tagnote.application.catalog.importer.model;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class SpotifyTrackMetadata {

    private final String spotifyTrackId;
    private final String title;
    private final String isrc;
    private final Integer durationMs;
    private final List<SpotifyArtistMetadata> trackArtists;
    private final String spotifyAlbumId;
    private final String albumTitle;
    private final Integer releaseYear;
    private final List<SpotifyArtistMetadata> albumArtists;

    public static SpotifyTrackMetadata of(
            String spotifyTrackId,
            String title,
            String isrc,
            Integer durationMs,
            List<SpotifyArtistMetadata> trackArtists,
            String spotifyAlbumId,
            String albumTitle,
            Integer releaseYear,
            List<SpotifyArtistMetadata> albumArtists
    ) {
        return SpotifyTrackMetadata.builder()
                .spotifyTrackId(spotifyTrackId)
                .title(title)
                .isrc(isrc)
                .durationMs(durationMs)
                .trackArtists(List.copyOf(trackArtists))
                .spotifyAlbumId(spotifyAlbumId)
                .albumTitle(albumTitle)
                .releaseYear(releaseYear)
                .albumArtists(List.copyOf(albumArtists))
                .build();
    }
}
