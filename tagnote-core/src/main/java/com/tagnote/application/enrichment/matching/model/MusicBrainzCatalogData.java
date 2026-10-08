package com.tagnote.application.enrichment.matching.model;

import java.util.List;

public final class MusicBrainzCatalogData {

    private MusicBrainzCatalogData() {
    }

    public record RecordingCandidate(
            String id,
            String title,
            Integer durationMs,
            List<String> artistNames
    ) {
        public RecordingCandidate {
            artistNames = artistNames == null ? List.of() : List.copyOf(artistNames);
        }
    }

    public record RecordingDetails(
            String id,
            List<Genre> genres,
            List<ReleaseGroupCandidate> releaseGroups
    ) {
        public RecordingDetails {
            genres = genres == null ? List.of() : List.copyOf(genres);
            releaseGroups = releaseGroups == null ? List.of() : List.copyOf(releaseGroups);
        }
    }

    public record ReleaseGroupCandidate(
            String id,
            String title,
            Integer releaseYear,
            List<String> artistNames
    ) {
        public ReleaseGroupCandidate {
            artistNames = artistNames == null ? List.of() : List.copyOf(artistNames);
        }
    }

    public record ReleaseGroupDetails(String id, List<Genre> genres) {
        public ReleaseGroupDetails {
            genres = genres == null ? List.of() : List.copyOf(genres);
        }
    }

    public record Genre(String name, Integer count) {
    }
}
