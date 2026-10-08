package com.tagnote.application.enrichment;

import com.tagnote.application.catalog.importer.model.ImportedAlbum;
import com.tagnote.application.catalog.importer.model.ImportedTrack;
import com.tagnote.application.enrichment.config.ExternalEnrichmentProperties;
import com.tagnote.application.enrichment.exception.ExternalProviderException;
import com.tagnote.application.enrichment.model.CatalogExternalIdentityMatch;
import com.tagnote.application.enrichment.model.CollectedExternalTags;
import com.tagnote.application.enrichment.model.ExternalTagInput;
import com.tagnote.application.enrichment.model.EnrichmentDeadline;
import com.tagnote.application.enrichment.model.ProviderEnrichmentResult;
import com.tagnote.application.enrichment.model.ProviderEnrichmentStatus;
import com.tagnote.application.enrichment.port.ExternalTagProvider;
import com.tagnote.domain.enrichment.assertion.EvidenceType;
import com.tagnote.domain.enrichment.observation.ExternalTagSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExternalEnrichmentCollectorTest {

    private final ExecutorService executor = Executors.newFixedThreadPool(3);

    @AfterEach
    void tearDown() {
        executor.shutdownNow();
    }

    @Test
    void 세_provider를_병렬_시작하고_source_순서로_결과를_병합한다() {
        CountDownLatch entered = new CountDownLatch(3);
        Function<ExternalTagSource, ProviderEnrichmentResult> result = source -> {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
            entered.countDown();
            await(entered);
            return ProviderEnrichmentResult.completed(
                    source,
                    new CollectedExternalTags(List.of(), List.of(input(source))),
                    source == ExternalTagSource.MUSICBRAINZ
                            ? new CatalogExternalIdentityMatch("recording-id")
                            : CatalogExternalIdentityMatch.none()
            );
        };
        ExternalEnrichmentCollector collector = collector(List.of(
                provider(ExternalTagSource.LASTFM, result),
                provider(ExternalTagSource.DISCOGS, result),
                provider(ExternalTagSource.MUSICBRAINZ, result)
        ), 1000);

        var collected = collector.collect(importedTrack());

        assertThat(collected.tags().trackInputs())
                .extracting(ExternalTagInput::source)
                .containsExactly(
                        ExternalTagSource.MUSICBRAINZ,
                        ExternalTagSource.DISCOGS,
                        ExternalTagSource.LASTFM
                );
        assertThat(collected.identityMatch())
                .isEqualTo(new CatalogExternalIdentityMatch("recording-id"));
    }

    @Test
    void provider_외부실패는_격리하고_다른_provider_결과를_보존한다() {
        ExternalTagProvider failed = provider(ExternalTagSource.DISCOGS, ignored -> {
            throw new ExternalProviderException(ProviderEnrichmentStatus.FAILED, "fixture failure");
        });
        ExternalTagProvider succeeded = provider(ExternalTagSource.LASTFM, source ->
                ProviderEnrichmentResult.completed(
                        source,
                        new CollectedExternalTags(List.of(), List.of(input(source))),
                        CatalogExternalIdentityMatch.none()
                ));

        var collected = collector(List.of(failed, succeeded), 1000).collect(importedTrack());

        assertThat(collected.tags().trackInputs()).singleElement()
                .extracting(ExternalTagInput::source)
                .isEqualTo(ExternalTagSource.LASTFM);
        assertThat(collected.providerResults())
                .extracting(ProviderEnrichmentResult::status)
                .containsExactly(ProviderEnrichmentStatus.FAILED, ProviderEnrichmentStatus.SUCCESS);
    }

    @Test
    void 전체_budget을_넘긴_provider는_timeout으로_반환한다() {
        CountDownLatch neverReleased = new CountDownLatch(1);
        ExternalTagProvider slow = provider(ExternalTagSource.MUSICBRAINZ, source -> {
            await(neverReleased);
            return ProviderEnrichmentResult.withoutData(source, ProviderEnrichmentStatus.EMPTY);
        });

        var collected = collector(List.of(slow), 30).collect(importedTrack());

        assertThat(collected.tags()).isEqualTo(CollectedExternalTags.empty());
        assertThat(collected.providerResults()).singleElement()
                .extracting(ProviderEnrichmentResult::status)
                .isEqualTo(ProviderEnrichmentStatus.TIMEOUT);
    }

    @Test
    void programmer_error는_provider_실패로_숨기지_않고_전파한다() {
        ExternalTagProvider broken = provider(ExternalTagSource.MUSICBRAINZ, source -> {
            throw new IllegalStateException("fixture programming error");
        });

        assertThatThrownBy(() -> collector(List.of(broken), 1000).collect(importedTrack()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("fixture programming error");
    }

    private ExternalEnrichmentCollector collector(List<ExternalTagProvider> providers, long budgetMs) {
        ExternalEnrichmentProperties properties = new ExternalEnrichmentProperties();
        properties.setTotalFirstLoadBudgetMs(budgetMs);
        return new ExternalEnrichmentCollector(providers, executor, properties);
    }

    private ExternalTagProvider provider(
            ExternalTagSource source,
            Function<ExternalTagSource, ProviderEnrichmentResult> collect
    ) {
        return new ExternalTagProvider() {
            @Override
            public ExternalTagSource source() {
                return source;
            }

            @Override
            public ProviderEnrichmentResult collect(ImportedTrack track, EnrichmentDeadline deadline) {
                return collect.apply(source);
            }
        };
    }

    private ExternalTagInput input(ExternalTagSource source) {
        return new ExternalTagInput(
                source,
                source.name(),
                source.name().toLowerCase() + ":fixture",
                source == ExternalTagSource.LASTFM ? EvidenceType.COMMUNITY_TAG : EvidenceType.EXPLICIT_GENRE,
                0.7
        );
    }

    private ImportedTrack importedTrack() {
        return ImportedTrack.of(
                1L, "spotify-track", null, "Track", "ISRC", 180_000, List.of(),
                ImportedAlbum.of(2L, "spotify-album", "Album", 2026, List.of())
        );
    }

    private void await(CountDownLatch latch) {
        try {
            if (!latch.await(2, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Latch did not complete");
            }
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
    }
}
