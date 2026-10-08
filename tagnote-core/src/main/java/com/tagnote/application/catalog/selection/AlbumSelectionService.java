package com.tagnote.application.catalog.selection;

import com.tagnote.application.catalog.detail.AlbumDetailReadService;
import com.tagnote.application.catalog.detail.TagPreviewService;
import com.tagnote.application.catalog.detail.model.AlbumDetail;
import com.tagnote.application.catalog.importer.AlbumImportService;
import com.tagnote.application.catalog.importer.model.ImportedAlbum;
import com.tagnote.application.enrichment.ExternalEnrichmentCollector;
import com.tagnote.application.enrichment.AlbumTagCompletionService;
import com.tagnote.application.enrichment.ObservationProcessingService;
import com.tagnote.application.enrichment.TagBootstrapService;
import com.tagnote.application.enrichment.model.CollectedExternalTags;
import com.tagnote.application.enrichment.model.ExternalTagInput;
import com.tagnote.application.enrichment.model.ObservationProcessingResult;
import com.tagnote.application.enrichment.model.ProviderEnrichmentStatus;
import com.tagnote.application.resolution.TagResolutionService;
import com.tagnote.domain.enrichment.subject.SubjectRef;
import com.tagnote.domain.taxonomy.matching.TagNameNormalizer;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AlbumSelectionService {
    private final AlbumImportService albumImportService;
    private final ExternalEnrichmentCollector collector;
    private final TagBootstrapService bootstrap;
    private final ObservationProcessingService observations;
    private final TagResolutionService resolver;
    private final AlbumDetailReadService details;
    private final TagPreviewService previews;
    private final AlbumTagCompletionService completion;
    private final TagNameNormalizer normalizer;

    public AlbumDetail select(String spotifyAlbumId) {
        ImportedAlbum album = albumImportService.importAlbum(spotifyAlbumId);
        AlbumDetail existing = details.findResolved(album).orElse(null);
        if (existing != null) return existing;
        if (completion.completed(album.getAlbumId())) {
            return new AlbumDetail(album, java.util.List.of()).withPreview(
                    previews.fromPersistedAlbum(album.getAlbumId()));
        }

        var enrichment = collector.collectAlbum(album);
        var inputs = enrichment.tags().albumInputs().stream().filter(this::persistable).toList();
        var matches = bootstrap.prepare(new CollectedExternalTags(inputs, java.util.List.of()));
        var processed = inputs.isEmpty() ? ObservationProcessingResult.empty()
                : observations.processPersistedAlbum(album, inputs, matches);
        var resolved = resolver.resolve(SubjectRef.album(album.getAlbumId()));
        if (resolved.isEmpty() && enrichment.providerResults().stream().allMatch(result ->
                result.status() == ProviderEnrichmentStatus.SUCCESS
                        || result.status() == ProviderEnrichmentStatus.EMPTY
                        || result.status() == ProviderEnrichmentStatus.NOT_FOUND)) {
            try {
                completion.complete(album.getAlbumId());
            } catch (DataIntegrityViolationException conflict) {
                if (!completion.completed(album.getAlbumId())) throw conflict;
            }
        }
        AlbumDetail detail = details.fromResolved(album, resolved);
        if (!resolved.isEmpty()) return detail;
        var preview = previews.fromProcessed(processed, ObservationProcessingResult.empty());
        return detail.withPreview(preview.isEmpty()
                ? previews.fromPersistedAlbum(album.getAlbumId()) : preview);
    }

    private boolean persistable(ExternalTagInput input) {
        return input.rawName().length() <= 255
                && input.externalRef().length() <= 255
                && normalizer.normalize(input.rawName()).value().length() <= 255;
    }
}
