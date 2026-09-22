package com.tagnote.application.resolution;

import com.tagnote.application.catalog.importer.model.ImportedTrack;
import com.tagnote.application.resolution.exception.ResolvedTagDuplicateException;
import com.tagnote.application.resolution.model.ResolvedTagResult;
import com.tagnote.domain.enrichment.subject.SubjectRef;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

@Slf4j
@Service
@RequiredArgsConstructor
public class TagResolutionService {

    private final TagResolutionWriteService writeService;

    public List<ResolvedTagResult> resolve(SubjectRef subject) {
        SubjectRef requiredSubject = Objects.requireNonNull(subject, "Subject must not be null");
        return resolveWithRetry(
                requiredSubject,
                () -> writeService.resolve(requiredSubject)
        );
    }

    public List<ResolvedTagResult> resolvePersistedAlbum(ImportedTrack track) {
        ImportedTrack requiredTrack = Objects.requireNonNull(track, "Imported track must not be null");
        SubjectRef subject = SubjectRef.album(requiredTrack.getAlbum().getAlbumId());
        return resolveWithRetry(
                subject,
                () -> writeService.resolvePersistedAlbum(subject.subjectId())
        );
    }

    public List<ResolvedTagResult> resolvePersistedTrack(ImportedTrack track) {
        ImportedTrack requiredTrack = Objects.requireNonNull(track, "Imported track must not be null");
        SubjectRef subject = SubjectRef.track(requiredTrack.getCatalogTrackId());
        return resolveWithRetry(
                subject,
                () -> writeService.resolvePersistedTrack(
                        subject.subjectId(), requiredTrack.getAlbum().getAlbumId()
                )
        );
    }

    private List<ResolvedTagResult> resolveWithRetry(
            SubjectRef subject,
            Supplier<List<ResolvedTagResult>> operation
    ) {
        try {
            return operation.get();
        } catch (ResolvedTagDuplicateException firstConflict) {
            log.warn(
                    "Retrying tag resolution after duplicate conflict: subjectType={}, subjectId={}",
                    subject.type(),
                    subject.subjectId()
            );
            return operation.get();
        }
    }
}
