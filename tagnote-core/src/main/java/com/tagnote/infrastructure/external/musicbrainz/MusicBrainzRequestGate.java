package com.tagnote.infrastructure.external.musicbrainz;

import com.tagnote.application.enrichment.config.ExternalEnrichmentProperties;
import com.tagnote.application.enrichment.exception.ExternalProviderException;
import com.tagnote.application.enrichment.model.ProviderEnrichmentStatus;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "tag.enrichment.musicbrainz", name = "enabled", havingValue = "true")
public class MusicBrainzRequestGate {

    private final long minimumIntervalNanos;
    private long lastStartedAtNanos = Long.MIN_VALUE;

    public MusicBrainzRequestGate(ExternalEnrichmentProperties properties) {
        this.minimumIntervalNanos = properties.getMusicbrainz().getMinimumRequestIntervalMs() * 1_000_000L;
    }

    public synchronized void awaitPermission() {
        long now = System.nanoTime();
        if (lastStartedAtNanos != Long.MIN_VALUE) {
            long remaining = minimumIntervalNanos - (now - lastStartedAtNanos);
            if (remaining > 0) {
                try {
                    long millis = remaining / 1_000_000L;
                    int nanos = (int) (remaining % 1_000_000L);
                    Thread.sleep(millis, nanos);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    throw new ExternalProviderException(
                            ProviderEnrichmentStatus.TIMEOUT,
                            "MusicBrainz request wait was interrupted",
                            interrupted
                    );
                }
            }
        }
        lastStartedAtNanos = System.nanoTime();
    }
}
