package com.tagnote.application.enrichment;

import com.tagnote.application.enrichment.exception.AssertionDuplicateException;
import com.tagnote.application.enrichment.exception.ObservationDuplicateException;
import com.tagnote.application.enrichment.model.ExternalTagInput;
import com.tagnote.application.enrichment.model.ObservationProcessingResult;
import com.tagnote.domain.enrichment.assertion.EvidenceType;
import com.tagnote.domain.enrichment.observation.ExternalTagSource;
import com.tagnote.domain.enrichment.subject.SubjectType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class ObservationProcessingServiceTest {

    @Mock
    private ObservationWriteService writeService;

    @InjectMocks
    private ObservationProcessingService processingService;

    @Test
    void write_transaction의_unique_충돌은_로그를_남기고_rollback_후_한번만_재시도한다(
            CapturedOutput output
    ) {
        List<ExternalTagInput> inputs = List.of(input());
        ObservationProcessingResult expected = new ObservationProcessingResult(0, 1, 1, 0, 0, 1);
        when(writeService.process(SubjectType.TRACK, 1L, inputs))
                .thenThrow(new ObservationDuplicateException(new RuntimeException("duplicate")))
                .thenReturn(expected);

        assertThat(processingService.process(SubjectType.TRACK, 1L, inputs)).isEqualTo(expected);
        verify(writeService, times(2)).process(SubjectType.TRACK, 1L, inputs);
        assertThat(output).contains(
                "Retrying observation processing after duplicate conflict",
                "subjectType=TRACK",
                "subjectId=1",
                "conflictType=ObservationDuplicateException"
        );
    }

    @Test
    void 재시도도_실패하면_예외를_전파한다() {
        List<ExternalTagInput> inputs = List.of(input());
        doThrow(new AssertionDuplicateException(new RuntimeException("duplicate")))
                .when(writeService).process(SubjectType.TRACK, 1L, inputs);

        assertThatThrownBy(() -> processingService.process(SubjectType.TRACK, 1L, inputs))
                .isInstanceOf(AssertionDuplicateException.class);
        verify(writeService, times(2)).process(SubjectType.TRACK, 1L, inputs);
    }

    @Test
    void duplicate이_아닌_예외는_즉시_전파한다() {
        List<ExternalTagInput> inputs = List.of(input());
        IllegalStateException unexpectedFailure = new IllegalStateException("unexpected");
        doThrow(unexpectedFailure)
                .when(writeService).process(SubjectType.TRACK, 1L, inputs);

        assertThatThrownBy(() -> processingService.process(SubjectType.TRACK, 1L, inputs))
                .isSameAs(unexpectedFailure);
        verify(writeService).process(SubjectType.TRACK, 1L, inputs);
    }

    @Test
    void write를_호출하기_전에_입력_collection의_null을_거부한다() {
        assertThatThrownBy(() -> processingService.process(
                SubjectType.TRACK,
                1L,
                Arrays.asList(input(), null)
        )).isInstanceOf(IllegalArgumentException.class);
    }

    private ExternalTagInput input() {
        return new ExternalTagInput(
                ExternalTagSource.MUSICBRAINZ,
                "Ambient",
                "recording:1",
                EvidenceType.EXPLICIT_GENRE,
                0.9
        );
    }
}
