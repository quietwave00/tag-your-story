package com.tagnote.infrastructure.external.musicbrainz;

import com.tagnote.application.enrichment.config.ExternalEnrichmentProperties;
import com.tagnote.application.enrichment.exception.ExternalProviderException;
import com.tagnote.application.enrichment.model.ProviderEnrichmentStatus;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MusicBrainzRequestGateTest {

    @Test
    void 단일_production_constructor로_spring_bean을_생성한다() {
        new ApplicationContextRunner()
                .withPropertyValues("tag.enrichment.musicbrainz.enabled=true")
                .withBean(ExternalEnrichmentProperties.class, ExternalEnrichmentProperties::new)
                .withBean(MusicBrainzRequestGate.class)
                .run(context -> assertThat(context).hasSingleBean(MusicBrainzRequestGate.class));
    }

    @Test
    void request_start간_대기중_interrupt를_복구하고_timeout으로_변환한다() {
        ExternalEnrichmentProperties properties = new ExternalEnrichmentProperties();
        properties.getMusicbrainz().setMinimumRequestIntervalMs(1000);
        MusicBrainzRequestGate gate = new MusicBrainzRequestGate(properties);

        gate.awaitPermission();
        Thread.currentThread().interrupt();
        try {
            assertThatThrownBy(gate::awaitPermission)
                    .isInstanceOfSatisfying(ExternalProviderException.class, failure ->
                            assertThat(failure.getStatus()).isEqualTo(ProviderEnrichmentStatus.TIMEOUT)
                    );

            assertThat(Thread.currentThread().isInterrupted()).isTrue();
        } finally {
            Thread.interrupted();
        }
    }
}
