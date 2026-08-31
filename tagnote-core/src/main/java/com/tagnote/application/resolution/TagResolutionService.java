package com.tagnote.application.resolution;

import com.tagnote.application.resolution.exception.ResolvedTagDuplicateException;
import com.tagnote.application.resolution.model.ResolvedTagResult;
import com.tagnote.domain.enrichment.subject.SubjectRef;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class TagResolutionService {

    private final TagResolutionWriteService writeService;

    public List<ResolvedTagResult> resolve(SubjectRef subject) {
        SubjectRef requiredSubject = Objects.requireNonNull(subject, "Subject must not be null");
        try {
            return writeService.resolve(requiredSubject);
        } catch (ResolvedTagDuplicateException firstConflict) {
            log.warn(
                    "Retrying tag resolution after duplicate conflict: subjectType={}, subjectId={}",
                    requiredSubject.type(),
                    requiredSubject.subjectId()
            );
            return writeService.resolve(requiredSubject);
        }
    }
}
