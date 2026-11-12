package com.tagnote.application.enrichment.port;

import com.tagnote.application.enrichment.model.EnrichmentDeadline;
import com.tagnote.application.enrichment.model.ExternalTagInput;
import com.tagnote.application.enrichment.model.ProviderEnrichmentStatus;
import com.tagnote.domain.enrichment.observation.ExternalTagSource;

import java.util.List;

public interface AlbumProviderEvidenceRepository {
    Claim acquire(long albumId, ExternalTagSource source, EnrichmentDeadline deadline);

    boolean owns(long albumId, ExternalTagSource source, String token);

    boolean complete(long albumId, ExternalTagSource source, String token,
                     ProviderEnrichmentStatus status, List<ExternalTagInput> tags);

    void release(long albumId, ExternalTagSource source, String token, ProviderEnrichmentStatus status);

    record Claim(ProviderEnrichmentStatus status, List<ExternalTagInput> tags,
                 String token, boolean reused) {}
}
