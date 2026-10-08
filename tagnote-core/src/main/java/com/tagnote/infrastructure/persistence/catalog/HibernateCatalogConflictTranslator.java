package com.tagnote.infrastructure.persistence.catalog;

import com.tagnote.application.catalog.importer.exception.CatalogDuplicateException;
import com.tagnote.application.catalog.importer.exception.CatalogDuplicateTarget;
import com.tagnote.application.catalog.importer.port.CatalogConflictTranslator;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class HibernateCatalogConflictTranslator implements CatalogConflictTranslator {

    private static final String ARTIST_UNIQUE_CONSTRAINT = "uk_artist_spotify_id";
    private static final String ALBUM_UNIQUE_CONSTRAINT = "uk_album_spotify_id";
    private static final String TRACK_UNIQUE_CONSTRAINT = "uk_track_spotify_id";

    @Override
    public RuntimeException translate(RuntimeException failure) {
        String constraintName = findConstraintName(failure);
        if (containsConstraint(constraintName, ARTIST_UNIQUE_CONSTRAINT)) {
            return new CatalogDuplicateException(CatalogDuplicateTarget.ARTIST, failure);
        }
        if (containsConstraint(constraintName, ALBUM_UNIQUE_CONSTRAINT)) {
            return new CatalogDuplicateException(CatalogDuplicateTarget.ALBUM, failure);
        }
        if (containsConstraint(constraintName, TRACK_UNIQUE_CONSTRAINT)) {
            return new CatalogDuplicateException(CatalogDuplicateTarget.TRACK, failure);
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
