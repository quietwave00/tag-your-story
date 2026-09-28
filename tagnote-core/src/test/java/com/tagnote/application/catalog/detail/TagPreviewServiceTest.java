package com.tagnote.application.catalog.detail;

import com.tagnote.application.enrichment.model.ObservationProcessingResult;
import com.tagnote.domain.enrichment.observation.ExternalTagSource;
import com.tagnote.infrastructure.persistence.enrichment.ExternalTagObservationJpaRepository;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class TagPreviewServiceTest {
    private final TagPreviewService service = new TagPreviewService(
            mock(ExternalTagObservationJpaRepository.class));

    @Test
    void preview는_source_우선순위로_정렬하고_normalized_name을_중복_제거해_다섯개만_반환한다() {
        ObservationProcessingResult album = result(List.of(
                row("Night Drive", "night drive", ExternalTagSource.LASTFM),
                row("Ambient", "ambient", ExternalTagSource.DISCOGS),
                row("A", "a", ExternalTagSource.LASTFM)));
        ObservationProcessingResult track = result(List.of(
                row("AMBIENT", "ambient", ExternalTagSource.MUSICBRAINZ),
                row("B", "b", ExternalTagSource.LASTFM),
                row("C", "c", ExternalTagSource.LASTFM),
                row("D", "d", ExternalTagSource.LASTFM)));

        var preview = service.fromProcessed(album, track);

        assertThat(preview).extracting(item -> item.name())
                .containsExactly("AMBIENT", "Night Drive", "A", "B", "C");
        assertThat(preview.get(0).source()).isEqualTo(ExternalTagSource.MUSICBRAINZ);
    }

    private ObservationProcessingResult result(List<ObservationProcessingResult.NewObservation> rows) {
        return new ObservationProcessingResult(0, 0, 0, rows.size(), 0, 0, rows);
    }

    private ObservationProcessingResult.NewObservation row(
            String raw, String normalized, ExternalTagSource source) {
        return new ObservationProcessingResult.NewObservation(raw, normalized, source);
    }
}
