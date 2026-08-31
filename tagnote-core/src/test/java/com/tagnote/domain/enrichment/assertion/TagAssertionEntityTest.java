package com.tagnote.domain.enrichment.assertion;

import com.tagnote.domain.enrichment.subject.SubjectRef;
import com.tagnote.domain.taxonomy.tag.TagEntity;
import com.tagnote.domain.taxonomy.tag.TagStatus;
import com.tagnote.domain.taxonomy.tag.TagType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TagAssertionEntityTest {

    @Test
    void pending_assertion을_approve하거나_reject할_수_있다() {
        TagAssertionEntity approved = assertion(AssertionStatus.PENDING, 0.8);
        TagAssertionEntity rejected = assertion(AssertionStatus.PENDING, 0.7);

        approved.approve();
        rejected.reject();

        assertThat(approved.getStatus()).isEqualTo(AssertionStatus.APPROVED);
        assertThat(rejected.getStatus()).isEqualTo(AssertionStatus.REJECTED);
        assertThatThrownBy(approved::reject).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void exact_match_assertion은_APPROVED로_생성한다() {
        TagAssertionEntity assertion = TagAssertionEntity.createApproved(
                SubjectRef.album(2L),
                tag(),
                AssertionSource.DISCOGS,
                EvidenceType.EXPLICIT_STYLE,
                0.9
        );

        assertThat(assertion.getStatus()).isEqualTo(AssertionStatus.APPROVED);
        assertThat(assertion.getInheritedFromAssertion()).isNull();
        assertThat(assertion.getCreatedAt()).isNotNull();
    }

    @Test
    void confidence_범위를_검증한다() {
        assertThatThrownBy(() -> assertion(AssertionStatus.PENDING, -0.01))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> assertion(AssertionStatus.PENDING, 1.01))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> assertion(AssertionStatus.PENDING, Double.NaN))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private TagAssertionEntity assertion(AssertionStatus status, double confidence) {
        return TagAssertionEntity.create(
                SubjectRef.track(1L),
                tag(),
                AssertionSource.MUSICBRAINZ,
                EvidenceType.EXPLICIT_GENRE,
                confidence,
                status
        );
    }

    private TagEntity tag() {
        return TagEntity.create("Ambient", "ambient", TagType.GENRE, TagStatus.ACTIVE, null);
    }
}
