package com.tagnote.application.catalog.importer.model;

import java.util.Objects;

public record CatalogUpsertResult(boolean created, ImportedTrack importedTrack) {

    public CatalogUpsertResult {
        if (created) {
            Objects.requireNonNull(importedTrack, "Created catalog result must include the imported track");
        } else if (importedTrack != null) {
            throw new IllegalArgumentException("Existing catalog result must be read from canonical storage");
        }
    }

    public static CatalogUpsertResult created(ImportedTrack importedTrack) {
        return new CatalogUpsertResult(true, importedTrack);
    }

    public static CatalogUpsertResult existing() {
        return new CatalogUpsertResult(false, null);
    }
}
