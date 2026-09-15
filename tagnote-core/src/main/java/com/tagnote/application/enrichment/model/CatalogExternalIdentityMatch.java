package com.tagnote.application.enrichment.model;

public record CatalogExternalIdentityMatch(
        String musicBrainzRecordingId,
        String musicBrainzReleaseGroupId
) {

    public static CatalogExternalIdentityMatch none() {
        return new CatalogExternalIdentityMatch(null, null);
    }

    public boolean isEmpty() {
        return musicBrainzRecordingId == null && musicBrainzReleaseGroupId == null;
    }
}
