package com.tagnote.application.catalog.importer.model;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SpotifyArtistMetadata {

    private final String spotifyArtistId;
    private final String name;
    private final int position;

    public static SpotifyArtistMetadata of(String spotifyArtistId, String name, int position) {
        return SpotifyArtistMetadata.builder()
                .spotifyArtistId(spotifyArtistId)
                .name(name)
                .position(position)
                .build();
    }
}
