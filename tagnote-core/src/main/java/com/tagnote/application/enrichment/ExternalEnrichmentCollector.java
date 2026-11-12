package com.tagnote.application.enrichment;

import com.tagnote.application.catalog.importer.model.ImportedTrack;
import com.tagnote.application.catalog.importer.model.ImportedAlbum;
import com.tagnote.application.enrichment.config.ExternalEnrichmentProperties;
import com.tagnote.application.enrichment.diagnostics.EnrichmentDiagnostics;
import com.tagnote.application.enrichment.exception.ExternalProviderException;
import com.tagnote.application.enrichment.model.CatalogExternalIdentityMatch;
import com.tagnote.application.enrichment.model.AlbumProviderEvidence;
import com.tagnote.application.enrichment.model.CollectedExternalTags;
import com.tagnote.application.enrichment.model.ExternalEnrichmentCollection;
import com.tagnote.application.enrichment.model.EnrichmentDeadline;
import com.tagnote.application.enrichment.model.ProviderEnrichmentResult;
import com.tagnote.application.enrichment.model.ProviderEnrichmentStatus;
import com.tagnote.application.enrichment.model.ExternalTagInput;
import com.tagnote.application.enrichment.port.ExternalTagProvider;
import com.tagnote.domain.enrichment.observation.ExternalTagSource;
import com.tagnote.application.enrichment.port.AlbumProviderEvidenceRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Autowired;
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
    private final AlbumProviderEvidenceRepository albumStore;

    @Autowired
    public ExternalEnrichmentCollector(
            List<ExternalTagProvider> providers,
            @Qualifier(EXECUTOR) ExecutorService executor,
            ExternalEnrichmentProperties properties,
            AlbumProviderEvidenceRepository albumStore
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
        this.albumStore = albumStore;
    }

    public ExternalEnrichmentCollector(List<ExternalTagProvider> providers,
                                       ExecutorService executor, ExternalEnrichmentProperties properties) {
        this(providers, executor, properties, null);
    }

    public ExternalEnrichmentCollection collect(ImportedTrack track) {
        if (albumStore != null) return collectWithAlbumReuse(
                (provider, deadline) -> collectTrackBranches(provider, track, deadline));
        return collect(track.getCatalogTrackId(),
                (provider, deadline) -> provider.collect(track, deadline));
    }

    public ExternalEnrichmentCollection collectAlbum(ImportedAlbum album) {
        if (albumStore != null) return collectWithAlbumReuse(
                (provider, deadline) -> collectAlbumBranch(provider, album, deadline));
        return collect(album.getAlbumId(),
                (provider, deadline) -> provider.collectAlbum(album, deadline));
    }

    public boolean completeAlbumEvidence(long albumId, ExternalEnrichmentCollection collection,
                                      List<ExternalTagInput> persistedInputs) {
        if (albumStore == null) return true;
        boolean allCompleted = true;
        for (AlbumProviderEvidence evidence : collection.albumEvidence()) {
            if (evidence.requiresCompletion()) {
                boolean completed = albumStore.complete(albumId, evidence.source(), evidence.leaseToken(),
                        evidence.status(), persistedInputs.stream()
                                .filter(input -> input.source() == evidence.source()).toList());
                allCompleted = completed && allCompleted;
            }
        }
        return allCompleted;
    }

    public boolean ownsAlbumEvidence(long albumId, ExternalEnrichmentCollection collection) {
        if (albumStore == null) return true;
        return collection.albumEvidence().stream().filter(AlbumProviderEvidence::requiresCompletion)
                .allMatch(evidence -> albumStore.owns(albumId, evidence.source(), evidence.leaseToken()));
    }

    public List<ExternalTagInput> usableAlbumInputs(long albumId, ExternalEnrichmentCollection collection) {
        if (albumStore == null || collection.albumEvidence().isEmpty()) return collection.tags().albumInputs();
        java.util.Set<ExternalTagSource> accepted = collection.albumEvidence().stream()
                .filter(evidence -> !evidence.requiresCompletion()
                        || albumStore.owns(albumId, evidence.source(), evidence.leaseToken()))
                .map(AlbumProviderEvidence::source).collect(java.util.stream.Collectors.toSet());
        return collection.tags().albumInputs().stream().filter(input -> accepted.contains(input.source())).toList();
    }

    private ExternalEnrichmentCollection collectWithAlbumReuse(
            BiFunction<ExternalTagProvider, EnrichmentDeadline, ProviderTask> operation) {
        if (providers.isEmpty()) return ExternalEnrichmentCollection.empty();
        EnrichmentDeadline deadline = EnrichmentDeadline.afterMillis(properties.getTotalFirstLoadBudgetMs());
        EnrichmentDiagnostics.Context context = EnrichmentDiagnostics.current();
        Map<ExternalTagSource, CompletableFuture<ProviderTask>> futures = new EnumMap<>(ExternalTagSource.class);
        Map<ExternalTagSource, Long> startedAt = new EnumMap<>(ExternalTagSource.class);
        for (ExternalTagProvider provider : providers) {
            startedAt.put(provider.source(), System.nanoTime());
            futures.put(provider.source(), CompletableFuture.supplyAsync(() -> {
                try (EnrichmentDiagnostics.Scope ignored = EnrichmentDiagnostics.install(context)) {
                    return operation.apply(provider, deadline);
                }
            }, executor));
        }
        try {
            CompletableFuture.allOf(futures.values().toArray(CompletableFuture[]::new))
                    .get(deadline.remainingMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException timeout) {
            // Completed branches remain usable; unfinished branches retain their lease until worker cleanup.
        } catch (InterruptedException interrupted) {
            futures.values().forEach(future -> future.cancel(true));
            Thread.currentThread().interrupt();
            throw new IllegalStateException("External enrichment was interrupted", interrupted);
        } catch (ExecutionException execution) {
            futures.values().forEach(future -> future.cancel(true));
            throw propagateUnexpected(execution.getCause());
        }
        List<ProviderEnrichmentResult> results = new ArrayList<>();
        List<AlbumProviderEvidence> evidence = new ArrayList<>();
        for (ExternalTagProvider provider : providers) {
            CompletableFuture<ProviderTask> future = futures.get(provider.source());
            if (!future.isDone()) {
                future.cancel(true);
                ProviderEnrichmentResult timeout = ProviderEnrichmentResult.withoutData(
                        provider.source(), ProviderEnrichmentStatus.TIMEOUT);
                diagnosticResult(timeout, TimeUnit.NANOSECONDS.toMillis(
                        System.nanoTime() - startedAt.get(provider.source())), true);
                results.add(timeout);
                continue;
            }
            ProviderTask task = future.join();
            results.add(task.result());
            evidence.add(task.album());
            diagnosticResult(task.result(), TimeUnit.NANOSECONDS.toMillis(
                    System.nanoTime() - startedAt.get(provider.source())),
                    task.result().status() == ProviderEnrichmentStatus.TIMEOUT);
        }
        ExternalEnrichmentCollection aggregated = aggregate(results);
        if (EnrichmentDiagnostics.enabled()) EnrichmentDiagnostics.event("enrichment_completed", Map.of(
                "providers", results.stream().map(result -> Map.of("source", result.source().name(),
                        "status", result.status().name(), "albumTagCount", result.tags().albumInputs().size(),
                        "trackTagCount", result.tags().trackInputs().size())).toList()));
        return new ExternalEnrichmentCollection(aggregated.tags(), aggregated.identityMatch(),
                aggregated.providerResults(), evidence);
    }

    private ProviderTask collectTrackBranches(ExternalTagProvider provider, ImportedTrack track,
                                              EnrichmentDeadline deadline) {
        ProviderEnrichmentResult direct = safeCollect(provider, track.getCatalogTrackId(), deadline,
                (current, limit) -> current.collectTrackDirect(track, limit));
        ImportedTrack identified = track.withMusicBrainzRecordingId(direct.identityMatch().musicBrainzRecordingId());
        AlbumProviderEvidence album = albumEvidence(provider, track.getAlbum().getAlbumId(), deadline,
                (current, limit) -> current.collectAlbumFromTrack(identified, limit));
        EnrichmentDiagnostics.event("provider_branches_completed", Map.of(
                "provider", provider.source().name(), "trackStatus", direct.status().name(),
                "albumStatus", album.status().name(), "albumReused", album.reused()));
        return new ProviderTask(combine(provider.source(), direct, album), album);
    }

    private ProviderTask collectAlbumBranch(ExternalTagProvider provider, ImportedAlbum album,
                                            EnrichmentDeadline deadline) {
        AlbumProviderEvidence evidence = albumEvidence(provider, album.getAlbumId(), deadline,
                (current, limit) -> current.collectAlbum(album, limit));
        ProviderEnrichmentResult result = new ProviderEnrichmentResult(provider.source(), evidence.status(),
                new CollectedExternalTags(evidence.tags(), List.of()), CatalogExternalIdentityMatch.none());
        return new ProviderTask(result, evidence);
    }

    private AlbumProviderEvidence albumEvidence(ExternalTagProvider provider, long albumId,
                                                EnrichmentDeadline deadline,
            BiFunction<ExternalTagProvider, EnrichmentDeadline, ProviderEnrichmentResult> operation) {
        AlbumProviderEvidenceRepository.Claim claim = albumStore.acquire(albumId, provider.source(), deadline);
        if (claim.reused()) {
            EnrichmentDiagnostics.event("album_evidence_reused", Map.of("provider", provider.source().name(),
                    "albumId", albumId, "status", claim.status().name(), "reason", "COMPLETED_BRANCH"));
            return new AlbumProviderEvidence(provider.source(), claim.status(), claim.tags(), null, true);
        }
        if (claim.token() == null) {
            return new AlbumProviderEvidence(provider.source(), ProviderEnrichmentStatus.TIMEOUT,
                    List.of(), null, false);
        }
        ProviderEnrichmentResult fetched;
        try {
            fetched = safeCollect(provider, albumId, deadline, operation);
        } catch (RuntimeException unexpected) {
            albumStore.release(albumId, provider.source(), claim.token(), ProviderEnrichmentStatus.FAILED);
            throw unexpected;
        }
        if (deadline.remainingMillis() == 0 || Thread.currentThread().isInterrupted()) {
            albumStore.release(albumId, provider.source(), claim.token(), ProviderEnrichmentStatus.TIMEOUT);
            return new AlbumProviderEvidence(provider.source(), ProviderEnrichmentStatus.TIMEOUT,
                    List.of(), null, false);
        }
        if (fetched.status() == ProviderEnrichmentStatus.FAILED
                || fetched.status() == ProviderEnrichmentStatus.TIMEOUT) {
            albumStore.release(albumId, provider.source(), claim.token(), fetched.status());
            return new AlbumProviderEvidence(provider.source(), fetched.status(), List.of(), null, false);
        }
        return new AlbumProviderEvidence(provider.source(), fetched.status(), fetched.tags().albumInputs(),
                claim.token(), false);
    }

    private ProviderEnrichmentResult safeCollect(ExternalTagProvider provider, long subjectId,
            EnrichmentDeadline deadline,
            BiFunction<ExternalTagProvider, EnrichmentDeadline, ProviderEnrichmentResult> operation) {
        return collectProvider(provider, subjectId, deadline, operation);
    }

    private ProviderEnrichmentResult combine(ExternalTagSource source, ProviderEnrichmentResult direct,
                                              AlbumProviderEvidence album) {
        ProviderEnrichmentStatus status;
        if (direct.status() == ProviderEnrichmentStatus.TIMEOUT || album.status() == ProviderEnrichmentStatus.TIMEOUT) {
            status = ProviderEnrichmentStatus.TIMEOUT;
        } else if (direct.status() == ProviderEnrichmentStatus.FAILED
                || album.status() == ProviderEnrichmentStatus.FAILED) {
            status = ProviderEnrichmentStatus.FAILED;
        } else if (!direct.tags().trackInputs().isEmpty() || !album.tags().isEmpty()) {
            status = ProviderEnrichmentStatus.SUCCESS;
        } else if (direct.status() == ProviderEnrichmentStatus.NOT_FOUND
                && album.status() == ProviderEnrichmentStatus.NOT_FOUND) {
            status = ProviderEnrichmentStatus.NOT_FOUND;
        } else status = ProviderEnrichmentStatus.EMPTY;
        return new ProviderEnrichmentResult(source, status,
                new CollectedExternalTags(album.tags(), direct.tags().trackInputs()), direct.identityMatch());
    }

    private record ProviderTask(ProviderEnrichmentResult result, AlbumProviderEvidence album) {}

    private ExternalEnrichmentCollection collect(
            long subjectId,
            BiFunction<ExternalTagProvider, EnrichmentDeadline, ProviderEnrichmentResult> operation
    ) {
        if (providers.isEmpty()) {
            EnrichmentDiagnostics.event("enrichment_completed", Map.of("providers", List.of(),
                    "reason", "NO_PROVIDERS"));
            return ExternalEnrichmentCollection.empty();
        }

        EnrichmentDeadline deadline = EnrichmentDeadline.afterMillis(properties.getTotalFirstLoadBudgetMs());
        EnrichmentDiagnostics.Context context = EnrichmentDiagnostics.current();
        Map<ExternalTagSource, CompletableFuture<ProviderEnrichmentResult>> futures = new EnumMap<>(
                ExternalTagSource.class
        );
        Map<ExternalTagSource, Long> startedAt = new EnumMap<>(ExternalTagSource.class);
        for (ExternalTagProvider provider : providers) {
            startedAt.put(provider.source(), System.nanoTime());
            futures.put(
                    provider.source(),
                    CompletableFuture.supplyAsync(() -> {
                        try (EnrichmentDiagnostics.Scope ignored = EnrichmentDiagnostics.install(context)) {
                            return collectProvider(provider, subjectId, deadline, operation);
                        }
                    }, executor)
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
                diagnosticResult(timeout, TimeUnit.NANOSECONDS.toMillis(
                        System.nanoTime() - startedAt.get(provider.source())), true);
                results.add(timeout);
                continue;
            }
            try {
                ProviderEnrichmentResult result = future.join();
                if (result.status() == ProviderEnrichmentStatus.SUCCESS
                        || result.status() == ProviderEnrichmentStatus.EMPTY) {
                    logResult(subjectId, result, null);
                }
                diagnosticResult(result, TimeUnit.NANOSECONDS.toMillis(
                        System.nanoTime() - startedAt.get(provider.source())),
                        result.status() == ProviderEnrichmentStatus.TIMEOUT);
                results.add(result);
            } catch (RuntimeException unexpected) {
                throw propagateUnexpected(unexpected.getCause() == null ? unexpected : unexpected.getCause());
            }
        }
        if (timedOut) {
            log.debug("External enrichment budget exhausted. catalogSubjectId={}", subjectId);
        }
        if (EnrichmentDiagnostics.enabled()) EnrichmentDiagnostics.event("enrichment_completed", Map.of("providers", results.stream()
                .map(result -> Map.of("source", result.source().name(), "status", result.status().name(),
                        "albumTagCount", result.tags().albumInputs().size(),
                        "trackTagCount", result.tags().trackInputs().size())).toList()));
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

    private void diagnosticResult(ProviderEnrichmentResult result, long elapsedMs, boolean timedOut) {
        if (!EnrichmentDiagnostics.enabled()) return;
        EnrichmentDiagnostics.event("provider_completed", Map.of(
                "provider", result.source().name(), "status", result.status().name(),
                "albumTagCount", result.tags().albumInputs().size(),
                "trackTagCount", result.tags().trackInputs().size(),
                "albumTags", EnrichmentDiagnostics.bounded(result.tags().albumInputs().stream()
                        .map(tag -> Map.of("name", tag.rawName(), "externalRef", tag.externalRef())).toList()),
                "trackTags", EnrichmentDiagnostics.bounded(result.tags().trackInputs().stream()
                        .map(tag -> Map.of("name", tag.rawName(), "externalRef", tag.externalRef())).toList()),
                "recordingId", result.identityMatch().musicBrainzRecordingId() == null
                        ? "" : result.identityMatch().musicBrainzRecordingId(),
                "elapsedMs", elapsedMs, "timedOut", timedOut));
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
