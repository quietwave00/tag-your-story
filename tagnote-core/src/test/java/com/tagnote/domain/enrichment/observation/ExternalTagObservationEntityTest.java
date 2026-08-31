package com.tagnote.domain.enrichment.observation;

import com.tagnote.domain.enrichment.subject.SubjectRef;
import com.tagnote.domain.taxonomy.matching.NormalizedTagName;
import com.tagnote.domain.taxonomy.tag.TagEntity;
import com.tagnote.domain.taxonomy.tag.TagStatus;
import com.tagnote.domain.taxonomy.tag.TagType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExternalTagObservationEntityTest {

    @Test
    void NEW_observation은_raw와_normalized_name을_분리해_보존한다() {
        ExternalTagObservationEntity observation = observation("  Ambient  ", "ambient");

        assertThat(observation.getRawName()).isEqualTo("  Ambient  ");
        assertThat(observation.getNormalizedName()).isEqualTo("ambient");
        assertThat(observation.getStatus()).isEqualTo(ObservationStatus.NEW);
        assertThat(observation.getMatchedTag()).isNull();
        assertThat(observation.getObservedAt()).isNotNull();
    }

    @Test
    void MATCHED_전이는_Tag를_필수로_하고_상태를_함께_변경한다() {
        ExternalTagObservationEntity observation = observation("Ambient", "ambient");
        TagEntity tag = tag();

        assertThatThrownBy(() -> observation.match(null))
                .isInstanceOf(NullPointerException.class);

        observation.match(tag);

        assertThat(observation.getStatus()).isEqualTo(ObservationStatus.MATCHED);
        assertThat(observation.getMatchedTag()).isSameAs(tag);
        assertThatThrownBy(() -> observation.match(tag))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void IGNORED_observation은_matched_Tag를_갖지_않는다() {
        ExternalTagObservationEntity observation = observation("Unknown", "unknown");

        observation.ignore();

        assertThat(observation.getStatus()).isEqualTo(ObservationStatus.IGNORED);
        assertThat(observation.getMatchedTag()).isNull();
    }

    @Test
    void blank_external_reference를_거부한다() {
        assertThatThrownBy(() -> ExternalTagObservationEntity.create(
                SubjectRef.track(1L),
                ExternalTagSource.MUSICBRAINZ,
                "Ambient",
                new NormalizedTagName("ambient"),
                " "
        )).isInstanceOf(IllegalArgumentException.class);
    }

    private ExternalTagObservationEntity observation(String rawName, String normalizedName) {
        return ExternalTagObservationEntity.create(
                SubjectRef.track(1L),
                ExternalTagSource.MUSICBRAINZ,
                rawName,
                new NormalizedTagName(normalizedName),
                "recording:1"
        );
    }

    private TagEntity tag() {
        return TagEntity.create("Ambient", "ambient", TagType.GENRE, TagStatus.ACTIVE, null);
    }
}
