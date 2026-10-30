package com.tagnote.application.catalog.detail.model;

import java.util.Locale;

public enum CatalogReleaseType {
    ALBUM,
    COMPILATION,
    SINGLE_EP;

    public static final long THIRTY_MINUTES_MS = 30L * 60 * 1000;

    public static CatalogReleaseType classify(String spotifyAlbumType, Integer totalTracks, Long observedDurationMs) {
        if (spotifyAlbumType == null) {
            return null;
        }
        return switch (spotifyAlbumType.toLowerCase(Locale.ROOT)) {
            case "compilation" -> COMPILATION;
            case "single" -> SINGLE_EP;
            case "album" -> {
                if (totalTracks == null || totalTracks <= 0) {
                    yield null;
                }
                if (totalTracks <= 6 || observedDurationMs != null && observedDurationMs < THIRTY_MINUTES_MS) {
                    yield SINGLE_EP;
                }
                yield observedDurationMs == null ? null : ALBUM;
            }
            default -> null;
        };
    }
}
