package com.tagnote.application.enrichment;

import com.tagnote.application.catalog.importer.model.ImportedTrack;
import com.tagnote.application.catalog.importer.model.ImportedAlbum;
import com.tagnote.application.enrichment.config.ExternalEnrichmentProperties;
import com.tagnote.application.enrichment.exception.ExternalProviderException;
import com.tagnote.application.enrichment.model.CatalogExternalIdentityMatch;
import com.tagnote.application.enrichment.model.CollectedExternalTags;
import com.tagnote.application.enrichment.model.ExternalEnrichmentCollection;
import com.tagnote.application.enrichment.model.EnrichmentDeadline;
import com.tagnote.application.enrichment.model.ProviderEnrichmentResult;
import com.tagnote.application.enrichment.model.ProviderEnrichmentStatus;
import com.tagnote.application.enrichment.port.ExternalTagProvider;
import com.tagnote.domain.enrichment.observation.ExternalTagSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BiFunction;

@Slf4j
@Service
public class ExternalEnrichmentCollector {

    public static final String EXECUTOR = "externalEnrichmentExecutor";

    private final List<ExternalTagProvider> providers;
    private final ExecutorService executor;
    private final ExternalEnrichmentProperties properties;

    public ExternalEnrichmentCollector(
            List<ExternalTagProvider> providers,
            @Qualifier(EXECUTOR) ExecutorService executor,
            ExternalEnrichmentProperties properties
    ) {
        this.providers = providers.stream()
                .sorted(Comparator.comparing(ExternalTagProvider::source))
                .toList();
        EnumMap<ExternalTagSource, ExternalTagProvider> uniqueProviders = new EnumMap<>(
                ExternalTagSource.class
        );
        for (ExternalTagProvider provider : this.providers) {
            if (uniqueProviders.put(provider.source(), provider) != null) {
                throw new IllegalStateException("Duplicate external tag provider: " + provider.source());
            }
        }
        this.executor = executor;
        this.properties = properties;
    }

    public ExternalEnrichmentCollection collect(ImportedTrack track) {
        return collect(track.getCatalogTrackId(),
                (provider, deadline) -> provider.collect(track, deadline));
    }

    public ExternalEnrichmentCollection collectAlbum(ImportedAlbum album) {
        return collect(album.getAlbumId(),
                (provider, deadline) -> provider.collectAlbum(album, deadline));
    }

    private ExternalEnrichmentCollection collect(
            long subjectId,
            BiFunction<ExternalTagProvider, EnrichmentDeadline, ProviderEnrichmentResult> operation
    ) {
        if (providers.isEmpty()) {
            return ExternalEnrichmentCollection.empty();
        }

        EnrichmentDeadline deadline = EnrichmentDeadline.afterMillis(properties.getTotalFirstLoadBudgetMs());
        Map<ExternalTagSource, CompletableFuture<ProviderEnrichmentResult>> futures = new EnumMap<>(
                ExternalTagSource.class
        );
        for (ExternalTagProvider provider : providers) {
            futures.put(
                    provider.source(),
                    CompletableFuture.supplyAsync(() -> collectProvider(provider, subjectId, deadline, operation), executor)
            );
        }

        CompletableFuture<Void> all = CompletableFuture.allOf(
                futures.values().toArray(CompletableFuture[]::new)
        );
        boolean timedOut = false;
        try {
            all.get(deadline.remainingMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException timeout) {
            timedOut = true;
        } catch (InterruptedException interrupted) {
            futures.values().forEach(future -> future.cancel(true));
            Thread.currentThread().interrupt();
            throw new IllegalStateException("External enrichment was interrupted", interrupted);
        } catch (ExecutionException execution) {
            futures.values().forEach(future -> future.cancel(true));
            throw propagateUnexpected(execution.getCause());
        }

        List<ProviderEnrichmentResult> results = new ArrayList<>();
        for (ExternalTagProvider provider : providers) {
            CompletableFuture<ProviderEnrichmentResult> future = futures.get(provider.source());
            if (!future.isDone()) {
                future.cancel(true);
                ProviderEnrichmentResult timeout = ProviderEnrichmentResult.withoutData(
                        provider.source(), ProviderEnrichmentStatus.TIMEOUT
                );
                logResult(subjectId, timeout, "total first-load budget exceeded");
                results.add(timeout);
                continue;
            }
            try {
                ProviderEnrichmentResult result = future.join();
                if (result.status() == ProviderEnrichmentStatus.SUCCESS
                        || result.status() == ProviderEnrichmentStatus.EMPTY) {
                    logResult(subjectId, result, null);
                }
                results.add(result);
            } catch (RuntimeException unexpected) {
                throw propagateUnexpected(unexpected.getCause() == null ? unexpected : unexpected.getCause());
            }
        }
        if (timedOut) {
            log.debug("External enrichment budget exhausted. catalogSubjectId={}", subjectId);
        }
        return aggregate(results);
    }

    private ProviderEnrichmentResult collectProvider(
            ExternalTagProvider provider,
            long subjectId,
            EnrichmentDeadline deadline,
            BiFunction<ExternalTagProvider, EnrichmentDeadline, ProviderEnrichmentResult> operation
    ) {
        try {
            ProviderEnrichmentResult result = operation.apply(provider, deadline);
            if (result.source() != provider.source()) {
                throw new IllegalStateException("Provider returned a mismatched source: " + provider.source());
            }
            return result;
        } catch (ExternalProviderException externalFailure) {
            ProviderEnrichmentResult result = ProviderEnrichmentResult.withoutData(
                    provider.source(), externalFailure.getStatus()
            );
            logResult(subjectId, result, externalFailure.getMessage());
            return result;
        }
    }

    private ExternalEnrichmentCollection aggregate(List<ProviderEnrichmentResult> results) {
        CollectedExternalTags tags = CollectedExternalTags.empty();
        String recordingId = null;
        for (ProviderEnrichmentResult result : results) {
            tags = tags.merge(result.tags());
            CatalogExternalIdentityMatch identity = result.identityMatch();
            recordingId = mergeIdentity(recordingId, identity.musicBrainzRecordingId(), "Recording");
        }
        return new ExternalEnrichmentCollection(
                tags,
                new CatalogExternalIdentityMatch(recordingId),
                results
        );
    }

    private String mergeIdentity(String current, String candidate, String type) {
        if (candidate == null) {
            return current;
        }
        if (current != null && !current.equals(candidate)) {
            throw new IllegalStateException("Conflicting MusicBrainz " + type + " matches");
        }
        return candidate;
    }

    private RuntimeException propagateUnexpected(Throwable cause) {
        if (cause instanceof RuntimeException runtime) {
            return runtime;
        }
        return new IllegalStateException("Unexpected external enrichment failure", cause);
    }

    private void logResult(long subjectId, ProviderEnrichmentResult result, String detail) {
        if (result.status() == ProviderEnrichmentStatus.SUCCESS
                || result.status() == ProviderEnrichmentStatus.EMPTY) {
            log.debug(
                    "External enrichment completed. provider={}, catalogSubjectId={}, status={}",
                    result.source(), subjectId, result.status()
            );
            return;
        }
        log.warn(
                "External enrichment branch failed. provider={}, catalogSubjectId={}, status={}, detail={}",
                result.source(), subjectId, result.status(), detail
        );
    }
}
