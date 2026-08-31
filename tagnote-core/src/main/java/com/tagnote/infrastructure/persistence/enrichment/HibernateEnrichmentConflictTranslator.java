package com.tagnote.infrastructure.persistence.enrichment;

import com.tagnote.application.enrichment.exception.AssertionDuplicateException;
import com.tagnote.application.enrichment.exception.ObservationDuplicateException;
import com.tagnote.application.enrichment.port.EnrichmentConflictTranslator;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class HibernateEnrichmentConflictTranslator implements EnrichmentConflictTranslator {

    private static final String OBSERVATION_UNIQUE_CONSTRAINT = "uk_external_tag_observation_identity";
    private static final String ASSERTION_UNIQUE_CONSTRAINT = "uk_tag_assertion_identity";

    @Override
    public RuntimeException translate(RuntimeException failure) {
        String constraintName = findConstraintName(failure);
        if (containsConstraint(constraintName, OBSERVATION_UNIQUE_CONSTRAINT)) {
            return new ObservationDuplicateException(failure);
        }
        if (containsConstraint(constraintName, ASSERTION_UNIQUE_CONSTRAINT)) {
            return new AssertionDuplicateException(failure);
        }
        return failure;
    }

    private String findConstraintName(Throwable failure) {
        Throwable cause = failure;
        while (cause != null) {
            if (cause instanceof ConstraintViolationException constraintViolation) {
                return constraintViolation.getConstraintName();
            }
            cause = cause.getCause();
        }
        return null;
    }

    private boolean containsConstraint(String actualName, String expectedName) {
        return actualName != null
                && actualName.toLowerCase(Locale.ROOT).contains(expectedName);
    }
}
