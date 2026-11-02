package com.tagnote.application.catalog.detail.model;

import java.util.List;

public record ArtistAlbumListing(List<SpotifyAlbumSummary> items, long totalCount,
                                 Boolean classificationComplete, Integer unclassifiedCount) {
    public ArtistAlbumListing {
        items = List.copyOf(items);
    }

    public static ArtistAlbumListing original(SpotifyCatalogPage<SpotifyAlbumSummary> page) {
        return new ArtistAlbumListing(page.items(), page.totalCount(), null, null);
    }

    public static ArtistAlbumListing indexed(ArtistAlbumIndexPage page) {
        return new ArtistAlbumListing(page.items(), page.totalCount(),
                page.classificationComplete(), page.unclassifiedCount());
    }
}
