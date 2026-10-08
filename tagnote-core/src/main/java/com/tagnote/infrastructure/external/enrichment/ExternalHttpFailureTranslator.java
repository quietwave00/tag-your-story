package com.tagnote.infrastructure.external.enrichment;

import com.tagnote.application.enrichment.exception.ExternalProviderException;
import com.tagnote.application.enrichment.model.ProviderEnrichmentStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.net.SocketTimeoutException;
import java.net.http.HttpTimeoutException;

public final class ExternalHttpFailureTranslator {

    private ExternalHttpFailureTranslator() {
    }

    public static ExternalProviderException translate(String provider, RestClientException failure) {
        if (failure instanceof ResourceAccessException && hasTimeoutCause(failure)) {
            return new ExternalProviderException(
                    ProviderEnrichmentStatus.TIMEOUT,
                    provider + " request timed out",
                    failure
            );
        }
        if (failure instanceof RestClientResponseException response) {
            HttpStatusCode status = response.getStatusCode();
            if (status.value() == 404) {
                return new ExternalProviderException(
                        ProviderEnrichmentStatus.NOT_FOUND,
                        provider + " entity was not found",
                        failure
                );
            }
            if (status.value() == 503) {
                return ExternalProviderException.retryable(
                        ProviderEnrichmentStatus.FAILED,
                        provider + " request failed with HTTP " + status.value(),
                        failure
                );
            }
            return new ExternalProviderException(
                    ProviderEnrichmentStatus.FAILED,
                    provider + " request failed with HTTP " + status.value(),
                    failure
            );
        }
        return new ExternalProviderException(
                ProviderEnrichmentStatus.FAILED,
                provider + " request failed",
                failure
        );
    }

    private static boolean hasTimeoutCause(Throwable failure) {
        Throwable current = failure;
        while (current != null) {
            if (current instanceof SocketTimeoutException || current instanceof HttpTimeoutException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
