package com.tagnote.application.enrichment.matching.model;

import java.util.List;

public final class DiscogsCatalogData {

    private DiscogsCatalogData() {
    }

    public enum EntityType {
        MASTER,
        RELEASE
    }

    public enum SearchField {
        RELEASE_TITLE,
        TRACK
    }

    public record AlbumSearchQuery(
            SearchField field,
            String value,
            List<String> artists
    ) {
        public AlbumSearchQuery {
            artists = artists == null ? List.of() : List.copyOf(artists);
        }
    }

    public record AlbumSearchAttempt(
            AlbumSearchQuery query,
            String validationTitle
    ) {
    }

    public record AlbumCandidate(
            long id,
            EntityType type,
            String title,
            Integer releaseYear,
            List<String> artistNames
    ) {
        public AlbumCandidate {
            artistNames = artistNames == null ? List.of() : List.copyOf(artistNames);
        }
    }

    public record AlbumDetails(
            long id,
            EntityType type,
            String title,
            Integer releaseYear,
            List<String> artistNames,
            List<String> genres,
            List<String> styles
    ) {
        public AlbumDetails {
            artistNames = artistNames == null ? List.of() : List.copyOf(artistNames);
            genres = genres == null ? List.of() : List.copyOf(genres);
            styles = styles == null ? List.of() : List.copyOf(styles);
        }
    }
}
