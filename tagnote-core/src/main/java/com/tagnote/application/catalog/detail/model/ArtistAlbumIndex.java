package com.tagnote.application.catalog.detail.model;

import java.util.List;

public record ArtistAlbumIndex(List<SpotifyAlbumSummary> allItems, List<Integer> albums,
                               List<Integer> singlesEps, List<Integer> compilations,
                               boolean classificationComplete, int unclassifiedCount,
                               long builtAt, int formatVersion) {
    public ArtistAlbumIndex {
        allItems = List.copyOf(allItems);
        albums = List.copyOf(albums);
        singlesEps = List.copyOf(singlesEps);
        compilations = List.copyOf(compilations);
    }

    public List<Integer> positions(String releaseType) {
        return switch (releaseType) {
            case "ALBUM" -> albums;
            case "SINGLE_EP" -> singlesEps;
            case "COMPILATION" -> compilations;
            default -> throw new IllegalArgumentException("Unknown release type");
        };
    }
}
