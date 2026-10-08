package com.tagnote.application.enrichment.model;

import java.util.List;

public record ExternalEnrichmentCollection(
        CollectedExternalTags tags,
        CatalogExternalIdentityMatch identityMatch,
        List<ProviderEnrichmentResult> providerResults
) {

    public ExternalEnrichmentCollection {
        providerResults = List.copyOf(providerResults);
    }

    public static ExternalEnrichmentCollection empty() {
        return new ExternalEnrichmentCollection(
                CollectedExternalTags.empty(),
                CatalogExternalIdentityMatch.none(),
                List.of()
        );
    }
}
