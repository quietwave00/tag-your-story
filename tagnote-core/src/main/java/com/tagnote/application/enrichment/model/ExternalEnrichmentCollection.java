package com.tagnote.application.enrichment.model;

import java.util.List;

public record ExternalEnrichmentCollection(
        CollectedExternalTags tags,
        CatalogExternalIdentityMatch identityMatch,
        List<ProviderEnrichmentResult> providerResults,
        List<AlbumProviderEvidence> albumEvidence
) {

    public ExternalEnrichmentCollection {
        providerResults = List.copyOf(providerResults);
        albumEvidence = List.copyOf(albumEvidence);
    }

    public ExternalEnrichmentCollection(CollectedExternalTags tags,
                                        CatalogExternalIdentityMatch identityMatch,
                                        List<ProviderEnrichmentResult> providerResults) {
        this(tags, identityMatch, providerResults, List.of());
    }

    public static ExternalEnrichmentCollection empty() {
        return new ExternalEnrichmentCollection(
                CollectedExternalTags.empty(),
                CatalogExternalIdentityMatch.none(),
                List.of()
        );
    }
}
