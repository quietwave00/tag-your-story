package com.tagnote.application.enrichment.model;

import com.tagnote.domain.enrichment.assertion.EvidenceType;
import com.tagnote.domain.enrichment.observation.ExternalTagSource;

import java.util.Objects;

public record ExternalTagInput(
        ExternalTagSource source,
        String rawName,
        String externalRef,
        EvidenceType evidenceType,
        double confidence
) {

    public ExternalTagInput {
        Objects.requireNonNull(source, "External tag source must not be null");
        requireText(rawName, "Raw tag name");
        requireText(externalRef, "External reference");
        Objects.requireNonNull(evidenceType, "Evidence type must not be null");
        if (!Double.isFinite(confidence) || confidence < 0.0 || confidence > 1.0) {
            throw new IllegalArgumentException("Confidence must be between 0.0 and 1.0");
        }
    }

    private static void requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
    }
}
