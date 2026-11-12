package com.tagnote.application.enrichment.model;

import com.tagnote.domain.enrichment.observation.ExternalTagSource;

import java.util.List;

public record AlbumProviderEvidence(ExternalTagSource source, ProviderEnrichmentStatus status,
                                    List<ExternalTagInput> tags, String leaseToken, boolean reused) {
    public AlbumProviderEvidence {
        tags = List.copyOf(tags);
    }

    public boolean requiresCompletion() {
        return leaseToken != null;
    }
}
