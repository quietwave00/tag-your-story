package com.tagnote.application.catalog.importer.model;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ImportedArtist {

    private final Long artistId;
    private final String spotifyArtistId;
    private final String name;
    private final int position;

    public static ImportedArtist of(Long artistId, String spotifyArtistId, String name, int position) {
        return ImportedArtist.builder()
                .artistId(artistId)
                .spotifyArtistId(spotifyArtistId)
                .name(name)
                .position(position)
                .build();
    }
}
