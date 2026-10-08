package com.tagnote.infrastructure.external.enrichment;

import com.tagnote.application.enrichment.exception.ExternalProviderException;
import com.tagnote.application.enrichment.model.ProviderEnrichmentStatus;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class ExternalHttpFailureTranslatorTest {

    @Test
    void timeout과_404와_429와_503을_provider_status와_재시도_여부로_분류한다() {
        ExternalProviderException timeout = ExternalHttpFailureTranslator.translate(
                "fixture",
                new ResourceAccessException("timeout", new SocketTimeoutException("fixture"))
        );
        ExternalProviderException notFound = ExternalHttpFailureTranslator.translate(
                "fixture",
                response(HttpStatus.NOT_FOUND)
        );
        ExternalProviderException rateLimited = ExternalHttpFailureTranslator.translate(
                "fixture",
                response(HttpStatus.TOO_MANY_REQUESTS)
        );
        ExternalProviderException unavailable = ExternalHttpFailureTranslator.translate(
                "fixture",
                response(HttpStatus.SERVICE_UNAVAILABLE)
        );

        assertThat(timeout.getStatus()).isEqualTo(ProviderEnrichmentStatus.TIMEOUT);
        assertThat(timeout.isRetryable()).isFalse();
        assertThat(notFound.getStatus()).isEqualTo(ProviderEnrichmentStatus.NOT_FOUND);
        assertThat(notFound.isRetryable()).isFalse();
        assertThat(rateLimited.getStatus()).isEqualTo(ProviderEnrichmentStatus.FAILED);
        assertThat(rateLimited.isRetryable()).isFalse();
        assertThat(unavailable.getStatus()).isEqualTo(ProviderEnrichmentStatus.FAILED);
        assertThat(unavailable.isRetryable()).isTrue();
    }

    private RestClientResponseException response(HttpStatus status) {
        if (status.is4xxClientError()) {
            return HttpClientErrorException.create(
                    status,
                    status.getReasonPhrase(),
                    HttpHeaders.EMPTY,
                    new byte[0],
                    StandardCharsets.UTF_8
            );
        }
        return HttpServerErrorException.create(
                status,
                status.getReasonPhrase(),
                HttpHeaders.EMPTY,
                new byte[0],
                StandardCharsets.UTF_8
        );
    }
}
