package com.tagnote.application.enrichment.exception;

import com.tagnote.application.enrichment.model.ProviderEnrichmentStatus;

public class ExternalProviderException extends RuntimeException {

    private final ProviderEnrichmentStatus status;
    private final boolean retryable;

    public ExternalProviderException(ProviderEnrichmentStatus status, String message) {
        this(status, message, null, false);
    }

    public ExternalProviderException(ProviderEnrichmentStatus status, String message, Throwable cause) {
        this(status, message, cause, false);
    }

    private ExternalProviderException(
            ProviderEnrichmentStatus status,
            String message,
            Throwable cause,
            boolean retryable
    ) {
        super(message, cause);
        this.status = status;
        this.retryable = retryable;
    }

    public static ExternalProviderException retryable(
            ProviderEnrichmentStatus status,
            String message
    ) {
        return new ExternalProviderException(status, message, null, true);
    }

    public static ExternalProviderException retryable(
            ProviderEnrichmentStatus status,
            String message,
            Throwable cause
    ) {
        return new ExternalProviderException(status, message, cause, true);
    }

    public ProviderEnrichmentStatus getStatus() {
        return status;
    }

    public boolean isRetryable() {
        return retryable;
    }
}
