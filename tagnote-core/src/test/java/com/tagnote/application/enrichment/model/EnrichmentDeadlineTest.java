package com.tagnote.application.enrichment.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EnrichmentDeadlineTest {

    @Test
    void 만료된_deadline은_새_작업시간을_허용하지_않는다() {
        EnrichmentDeadline deadline = EnrichmentDeadline.expired();

        assertThat(deadline.remainingMillis()).isZero();
        assertThat(deadline.hasTimeFor(0)).isFalse();
    }

    @Test
    void 남은시간이_요청비용보다_크면_작업을_허용한다() {
        EnrichmentDeadline deadline = EnrichmentDeadline.afterMillis(60_000);

        assertThat(deadline.remainingMillis()).isPositive();
        assertThat(deadline.hasTimeFor(1_000)).isTrue();
    }
}
