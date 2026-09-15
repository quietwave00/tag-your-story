package com.tagnote.infrastructure.external.enrichment;

import com.tagnote.application.enrichment.config.ExternalEnrichmentProperties;
import com.tagnote.application.enrichment.ExternalEnrichmentCollector;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

@Configuration
public class ExternalEnrichmentConfig {

    @Bean(name = ExternalEnrichmentCollector.EXECUTOR, destroyMethod = "shutdown")
    ExecutorService externalEnrichmentExecutor(ExternalEnrichmentProperties properties) {
        AtomicInteger sequence = new AtomicInteger();
        return Executors.newFixedThreadPool(properties.getExecutorSize(), runnable -> {
            Thread thread = new Thread(runnable);
            thread.setName("external-enrichment-" + sequence.incrementAndGet());
            return thread;
        });
    }

    @Bean("musicBrainzRestClient")
    @ConditionalOnProperty(prefix = "tag.enrichment.musicbrainz", name = "enabled", havingValue = "true")
    RestClient musicBrainzRestClient(ExternalEnrichmentProperties properties) {
        ExternalEnrichmentProperties.MusicBrainz config = properties.getMusicbrainz();
        return restClient(config.getBaseUrl(), config.getConnectTimeoutMs(), config.getReadTimeoutMs())
                .defaultHeader("User-Agent", config.getUserAgent())
                .build();
    }

    @Bean("discogsRestClient")
    @ConditionalOnProperty(prefix = "tag.enrichment.discogs", name = "enabled", havingValue = "true")
    RestClient discogsRestClient(ExternalEnrichmentProperties properties) {
        ExternalEnrichmentProperties.Discogs config = properties.getDiscogs();
        return restClient(config.getBaseUrl(), config.getConnectTimeoutMs(), config.getReadTimeoutMs())
                .defaultHeader("User-Agent", config.getUserAgent())
                .build();
    }

    @Bean("lastFmRestClient")
    @ConditionalOnProperty(prefix = "tag.enrichment.lastfm", name = "enabled", havingValue = "true")
    RestClient lastFmRestClient(ExternalEnrichmentProperties properties) {
        ExternalEnrichmentProperties.LastFm config = properties.getLastfm();
        return restClient(config.getBaseUrl(), config.getConnectTimeoutMs(), config.getReadTimeoutMs()).build();
    }

    private RestClient.Builder restClient(String baseUrl, int connectTimeoutMs, int readTimeoutMs) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(connectTimeoutMs);
        requestFactory.setReadTimeout(readTimeoutMs);
        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory);
    }
}
