package com.tagnote.infrastructure.persistence.enrichment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tagnote.application.enrichment.model.EnrichmentDeadline;
import com.tagnote.application.enrichment.model.ExternalTagInput;
import com.tagnote.application.enrichment.model.ProviderEnrichmentStatus;
import com.tagnote.application.enrichment.port.AlbumProviderEvidenceRepository;
import com.tagnote.domain.enrichment.assertion.EvidenceType;
import com.tagnote.domain.enrichment.observation.ExternalTagSource;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class AlbumProviderEvidenceStoreTest {

    @Test
    void 완료된_빈_Album_branch와_미처리_상태를_구분해_재사용한다() {
        AlbumProviderEvidenceStore store = store();
        AlbumProviderEvidenceRepository.Claim first = store.acquire(1L, ExternalTagSource.LASTFM,
                EnrichmentDeadline.afterMillis(1000));
        assertThat(first.token()).isNotNull();
        store.complete(1L, ExternalTagSource.LASTFM, first.token(), ProviderEnrichmentStatus.EMPTY, List.of());

        AlbumProviderEvidenceRepository.Claim reused = store.acquire(1L, ExternalTagSource.LASTFM,
                EnrichmentDeadline.afterMillis(1000));
        assertThat(reused.reused()).isTrue();
        assertThat(reused.status()).isEqualTo(ProviderEnrichmentStatus.EMPTY);
        assertThat(reused.tags()).isEmpty();
    }

    @Test
    void 실패한_branch만_새_lease로_재시도하고_늦은_소유자는_완료할_수_없다() {
        AlbumProviderEvidenceStore store = store();
        AlbumProviderEvidenceRepository.Claim failed = store.acquire(1L, ExternalTagSource.DISCOGS,
                EnrichmentDeadline.afterMillis(1000));
        store.release(1L, ExternalTagSource.DISCOGS, failed.token(), ProviderEnrichmentStatus.FAILED);
        AlbumProviderEvidenceRepository.Claim retry = store.acquire(1L, ExternalTagSource.DISCOGS,
                EnrichmentDeadline.afterMillis(1000));

        assertThat(retry.token()).isNotEqualTo(failed.token());
        assertThat(store.owns(1L, ExternalTagSource.DISCOGS, failed.token())).isFalse();
        List<ExternalTagInput> tags = List.of(new ExternalTagInput(ExternalTagSource.DISCOGS,
                "Rock", "discogs:master:1", EvidenceType.EXPLICIT_GENRE, 0.8));
        store.complete(1L, ExternalTagSource.DISCOGS, failed.token(), ProviderEnrichmentStatus.SUCCESS, tags);
        assertThat(store.owns(1L, ExternalTagSource.DISCOGS, retry.token())).isTrue();
        store.complete(1L, ExternalTagSource.DISCOGS, retry.token(), ProviderEnrichmentStatus.SUCCESS, tags);

        AlbumProviderEvidenceRepository.Claim reused = store.acquire(1L, ExternalTagSource.DISCOGS,
                EnrichmentDeadline.afterMillis(1000));
        assertThat(reused.tags()).containsExactlyElementsOf(tags);
    }

    @Test
    void 같은_Album의_동시_최초_요청은_하나의_lease만_획득한다() throws Exception {
        AlbumProviderEvidenceStore store = store();
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<AlbumProviderEvidenceRepository.Claim> first = executor.submit(() -> {
                start.await();
                return store.acquire(7L, ExternalTagSource.MUSICBRAINZ,
                        EnrichmentDeadline.afterMillis(100));
            });
            Future<AlbumProviderEvidenceRepository.Claim> second = executor.submit(() -> {
                start.await();
                return store.acquire(7L, ExternalTagSource.MUSICBRAINZ,
                        EnrichmentDeadline.afterMillis(100));
            });
            start.countDown();
            AlbumProviderEvidenceRepository.Claim left = first.get(2, TimeUnit.SECONDS);
            AlbumProviderEvidenceRepository.Claim right = second.get(2, TimeUnit.SECONDS);

            assertThat(List.of(left, right).stream().filter(claim -> claim.token() != null).count()).isEqualTo(1);
            assertThat(List.of(left, right).stream()
                    .filter(claim -> claim.status() == ProviderEnrichmentStatus.TIMEOUT).count()).isEqualTo(1);
        } finally {
            executor.shutdownNow();
        }
    }

    private AlbumProviderEvidenceStore store() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:album-evidence-" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1", "sa", "");
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("""
                create table album_provider_evidence (
                    album_id bigint not null,
                    source varchar(32) not null,
                    status varchar(32) not null,
                    raw_tags clob,
                    lease_token varchar(36),
                    lease_until timestamp,
                    completed_at timestamp,
                    primary key (album_id, source)
                )
                """);
        return new AlbumProviderEvidenceStore(jdbc, new ObjectMapper(),
                new DataSourceTransactionManager(dataSource));
    }
}
