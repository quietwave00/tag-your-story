package com.tagnote.application.enrichment.model;

import com.tagnote.domain.enrichment.observation.ExternalTagSource;

import java.util.Objects;

public record ProviderEnrichmentResult(
        ExternalTagSource source,
        ProviderEnrichmentStatus status,
        CollectedExternalTags tags,
        CatalogExternalIdentityMatch identityMatch
) {

    public ProviderEnrichmentResult {
        Objects.requireNonNull(source, "Provider source must not be null");
        Objects.requireNonNull(status, "Provider status must not be null");
        Objects.requireNonNull(tags, "Collected tags must not be null");
        Objects.requireNonNull(identityMatch, "Identity match must not be null");
    }

    public static ProviderEnrichmentResult completed(
            ExternalTagSource source,
            CollectedExternalTags tags,
            CatalogExternalIdentityMatch identityMatch
    ) {
        ProviderEnrichmentStatus status = tags.albumInputs().isEmpty() && tags.trackInputs().isEmpty()
                ? ProviderEnrichmentStatus.EMPTY
                : ProviderEnrichmentStatus.SUCCESS;
        return new ProviderEnrichmentResult(source, status, tags, identityMatch);
    }

    public static ProviderEnrichmentResult withoutData(
            ExternalTagSource source,
            ProviderEnrichmentStatus status
    ) {
        if (status == ProviderEnrichmentStatus.SUCCESS) {
            throw new IllegalArgumentException("SUCCESS result requires collected data");
        }
        return new ProviderEnrichmentResult(
                source,
                status,
                CollectedExternalTags.empty(),
                CatalogExternalIdentityMatch.none()
        );
    }
}
