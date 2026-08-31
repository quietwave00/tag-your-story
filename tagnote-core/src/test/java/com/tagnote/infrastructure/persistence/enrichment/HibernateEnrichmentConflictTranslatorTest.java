package com.tagnote.infrastructure.persistence.enrichment;

import com.tagnote.application.enrichment.exception.AssertionDuplicateException;
import com.tagnote.application.enrichment.exception.ObservationDuplicateException;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

class HibernateEnrichmentConflictTranslatorTest {

    private final HibernateEnrichmentConflictTranslator translator =
            new HibernateEnrichmentConflictTranslator();

    @Test
    void Observation_unique_위반을_의미_예외로_번역한다() {
        RuntimeException failure = violation("PUBLIC.UK_EXTERNAL_TAG_OBSERVATION_IDENTITY_INDEX");

        RuntimeException translated = translator.translate(failure);

        assertThat(translated).isInstanceOf(ObservationDuplicateException.class);
        assertThat(translated.getCause()).isSameAs(failure);
    }

    @Test
    void Assertion_unique_위반을_의미_예외로_번역한다() {
        RuntimeException failure = violation("uk_tag_assertion_identity");

        RuntimeException translated = translator.translate(failure);

        assertThat(translated).isInstanceOf(AssertionDuplicateException.class);
        assertThat(translated.getCause()).isSameAs(failure);
    }

    @Test
    void 알지_못하는_제약_위반은_원래_예외를_유지한다() {
        RuntimeException failure = violation("fk_matched_tag");

        assertThat(translator.translate(failure)).isSameAs(failure);
    }

    private DataIntegrityViolationException violation(String constraintName) {
        SQLException sqlException = new SQLException("constraint violation", "23000");
        ConstraintViolationException hibernateException = new ConstraintViolationException(
                "constraint violation",
                sqlException,
                constraintName
        );
        return new DataIntegrityViolationException("constraint violation", hibernateException);
    }
}
