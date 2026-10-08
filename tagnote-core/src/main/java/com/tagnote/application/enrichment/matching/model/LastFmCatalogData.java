package com.tagnote.application.enrichment.matching.model;

import java.util.List;

public final class LastFmCatalogData {

    private LastFmCatalogData() {
    }

    public record TopTags(
            String artistName,
            String title,
            List<Tag> tags
    ) {
        public TopTags {
            tags = tags == null ? List.of() : List.copyOf(tags);
        }
    }

    public record Tag(String name, int count) {
    }
}
