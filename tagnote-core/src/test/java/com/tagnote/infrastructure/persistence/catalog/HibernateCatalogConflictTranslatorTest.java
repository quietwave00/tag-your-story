package com.tagnote.infrastructure.persistence.catalog;

import com.tagnote.application.catalog.importer.exception.CatalogDuplicateException;
import com.tagnote.application.catalog.importer.exception.CatalogDuplicateTarget;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

class HibernateCatalogConflictTranslatorTest {

    private final HibernateCatalogConflictTranslator translator = new HibernateCatalogConflictTranslator();

    @Test
    void Catalog_Spotify_ID_unique_위반을_대상별_의미_예외로_번역한다() {
        assertDuplicateTarget("PUBLIC.UK_ARTIST_SPOTIFY_ID_INDEX", CatalogDuplicateTarget.ARTIST);
        assertDuplicateTarget("uk_album_spotify_id", CatalogDuplicateTarget.ALBUM);
        assertDuplicateTarget("Uk_TrAcK_SpOtIfY_Id", CatalogDuplicateTarget.TRACK);
    }

    @Test
    void 대상_외_constraint와_constraint_name_없는_failure는_그대로_유지한다() {
        RuntimeException foreignKeyFailure = violation("fk_track_album");
        RuntimeException unnamedFailure = new DataIntegrityViolationException("not null");

        assertThat(translator.translate(foreignKeyFailure)).isSameAs(foreignKeyFailure);
        assertThat(translator.translate(unnamedFailure)).isSameAs(unnamedFailure);
    }

    private void assertDuplicateTarget(String constraintName, CatalogDuplicateTarget expectedTarget) {
        RuntimeException failure = violation(constraintName);

        RuntimeException translated = translator.translate(failure);

        assertThat(translated).isInstanceOf(CatalogDuplicateException.class)
                .extracting(exception -> ((CatalogDuplicateException) exception).getTarget())
                .isEqualTo(expectedTarget);
        assertThat(translated.getCause()).isSameAs(failure);
    }

    private DataIntegrityViolationException violation(String constraintName) {
        SQLException sqlException = new SQLException("constraint violation", "23000");
        ConstraintViolationException hibernateException = new ConstraintViolationException(
                "constraint violation", sqlException, constraintName
        );
        return new DataIntegrityViolationException("constraint violation", hibernateException);
    }
}
