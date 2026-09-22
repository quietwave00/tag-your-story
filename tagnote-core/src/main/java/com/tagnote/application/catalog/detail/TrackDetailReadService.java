package com.tagnote.application.catalog.detail;

import com.tagnote.application.catalog.detail.model.SystemTagDetail;
import com.tagnote.application.catalog.detail.model.TrackDetail;
import com.tagnote.application.catalog.importer.model.ImportedTrack;
import com.tagnote.application.resolution.model.ResolvedTagResult;
import com.tagnote.domain.enrichment.subject.SubjectType;
import com.tagnote.domain.resolution.ResolvedStatus;
import com.tagnote.domain.resolution.SubjectTagResolvedEntity;
import com.tagnote.infrastructure.persistence.resolution.SubjectTagResolvedJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TrackDetailReadService {

    private final SubjectTagResolvedJpaRepository resolvedRepository;

    public Optional<TrackDetail> findResolved(ImportedTrack track) {
        List<SubjectTagResolvedEntity> resolved = resolvedRepository
                .findAllBySubjectWithTag(SubjectType.TRACK, track.getCatalogTrackId());
        if (resolved.isEmpty()) {
            return Optional.empty();
        }
        List<SystemTagDetail> systemTags = resolved.stream()
                .filter(row -> row.getStatus() != ResolvedStatus.HIDDEN)
                .sorted(Comparator.comparingDouble(
                                SubjectTagResolvedEntity::getScore
                        ).reversed()
                        .thenComparingLong(row -> row.getTag().getTagId()))
                .map(row -> new SystemTagDetail(
                        row.getTag().getTagId(),
                        row.getTag().getName(),
                        row.getScore()
                ))
                .toList();
        return Optional.of(new TrackDetail(track, systemTags));
    }

    public TrackDetail fromResolved(ImportedTrack track, List<ResolvedTagResult> resolved) {
        List<SystemTagDetail> systemTags = resolved.stream()
                .filter(result -> result.status() != ResolvedStatus.HIDDEN)
                .map(result -> new SystemTagDetail(
                        result.tagId(), result.tagName(), result.score()
                ))
                .toList();
        return new TrackDetail(track, systemTags);
    }
}
