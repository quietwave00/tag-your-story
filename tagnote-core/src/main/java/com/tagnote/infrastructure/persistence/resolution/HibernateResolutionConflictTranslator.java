package com.tagnote.infrastructure.persistence.resolution;

import com.tagnote.application.resolution.exception.ResolvedTagDuplicateException;
import com.tagnote.application.resolution.port.ResolutionConflictTranslator;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class HibernateResolutionConflictTranslator implements ResolutionConflictTranslator {

    private static final String RESOLVED_UNIQUE_CONSTRAINT = "uk_subject_tag_resolved_identity";
    private static final String ASSERTION_UNIQUE_CONSTRAINT = "uk_tag_assertion_identity";

    @Override
    public RuntimeException translate(RuntimeException failure) {
        String constraintName = findConstraintName(failure);
        if (constraintName != null && isRetryableDuplicateConstraint(constraintName)) {
            return new ResolvedTagDuplicateException(failure);
        }
        return failure;
    }

    private boolean isRetryableDuplicateConstraint(String constraintName) {
        String normalized = constraintName.toLowerCase(Locale.ROOT);
        return normalized.contains(RESOLVED_UNIQUE_CONSTRAINT)
                || normalized.contains(ASSERTION_UNIQUE_CONSTRAINT);
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
}
