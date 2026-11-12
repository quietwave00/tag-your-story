package com.tagnote.application.enrichment.port;

import com.tagnote.application.catalog.importer.model.ImportedTrack;
import com.tagnote.application.catalog.importer.model.ImportedAlbum;
import com.tagnote.application.enrichment.model.EnrichmentDeadline;
import com.tagnote.application.enrichment.model.ProviderEnrichmentResult;
import com.tagnote.application.enrichment.model.ProviderEnrichmentStatus;
import com.tagnote.domain.enrichment.observation.ExternalTagSource;
import com.tagnote.application.enrichment.model.CollectedExternalTags;

public interface ExternalTagProvider {

    ExternalTagSource source();

    ProviderEnrichmentResult collect(ImportedTrack track, EnrichmentDeadline deadline);

    default ProviderEnrichmentResult collectTrackDirect(ImportedTrack track, EnrichmentDeadline deadline) {
        ProviderEnrichmentResult result = collect(track, deadline);
        return new ProviderEnrichmentResult(source(), result.status(),
                new CollectedExternalTags(java.util.List.of(), result.tags().trackInputs()),
                result.identityMatch());
    }

    default ProviderEnrichmentResult collectAlbumFromTrack(ImportedTrack track, EnrichmentDeadline deadline) {
        return collectAlbum(track.getAlbum(), deadline);
    }

    default ProviderEnrichmentResult collectAlbum(ImportedAlbum album, EnrichmentDeadline deadline) {
        return ProviderEnrichmentResult.withoutData(source(), ProviderEnrichmentStatus.EMPTY);
    }
}
