package com.tagnote.application.enrichment;

import com.tagnote.application.enrichment.model.ExternalTagInput;
import com.tagnote.application.enrichment.model.ObservationProcessingResult;
import com.tagnote.application.enrichment.exception.AssertionDuplicateException;
import com.tagnote.application.enrichment.exception.ObservationDuplicateException;
import com.tagnote.domain.enrichment.subject.SubjectRef;
import com.tagnote.domain.enrichment.subject.SubjectType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class ObservationProcessingService {

    private final ObservationWriteService observationWriteService;

    public ObservationProcessingResult process(
            SubjectType subjectType,
            long subjectId,
            List<ExternalTagInput> inputs
    ) {
        Objects.requireNonNull(subjectType, "Subject type must not be null");
        if (subjectId <= 0) {
            throw new IllegalArgumentException("Subject ID must be positive");
        }
        List<ExternalTagInput> stableInputs = validateAndCopy(inputs);

        try {
            return observationWriteService.process(subjectType, subjectId, stableInputs);
        } catch (ObservationDuplicateException | AssertionDuplicateException firstConflict) {
            log.warn(
                    "Retrying observation processing after duplicate conflict: "
                            + "subjectType={}, subjectId={}, conflictType={}",
                    subjectType,
                    subjectId,
                    firstConflict.getClass().getSimpleName()
            );
            return observationWriteService.process(subjectType, subjectId, stableInputs);
        }
    }

    public ObservationProcessingResult process(SubjectRef subject, List<ExternalTagInput> inputs) {
        SubjectRef requiredSubject = Objects.requireNonNull(subject, "Subject must not be null");
        return process(requiredSubject.type(), requiredSubject.subjectId(), inputs);
    }

    private List<ExternalTagInput> validateAndCopy(List<ExternalTagInput> inputs) {
        Objects.requireNonNull(inputs, "External tag inputs must not be null");
        if (inputs.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("External tag inputs must not contain null");
        }
        return List.copyOf(inputs);
    }
}
