package com.tagnote.application.enrichment.model;

public record CatalogExternalIdentityMatch(String musicBrainzRecordingId) {

    public static CatalogExternalIdentityMatch none() {
        return new CatalogExternalIdentityMatch(null);
    }

    public boolean isEmpty() {
        return musicBrainzRecordingId == null;
    }
}
