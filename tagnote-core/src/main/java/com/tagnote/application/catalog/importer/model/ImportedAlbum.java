package com.tagnote.application.catalog.importer.model;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ImportedAlbum {

    private final Long albumId;
    private final String spotifyAlbumId;
    private final String title;
    private final Integer releaseYear;
    private final List<ImportedArtist> artists;

    public static ImportedAlbum of(
            Long albumId,
            String spotifyAlbumId,
            String title,
            Integer releaseYear,
            List<ImportedArtist> artists
    ) {
        return ImportedAlbum.builder()
                .albumId(albumId)
                .spotifyAlbumId(spotifyAlbumId)
                .title(title)
                .releaseYear(releaseYear)
                .artists(List.copyOf(artists))
                .build();
    }
}
