package com.tagnote.application.catalog.detail.model;

import java.util.List;

public record ArtistAlbumIndexPage(List<SpotifyAlbumSummary> items, long totalCount,
                                   boolean classificationComplete, int unclassifiedCount) {
    public ArtistAlbumIndexPage {
        items = List.copyOf(items);
    }
}
