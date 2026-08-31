package com.tagnote.application.catalog.detail;

import com.tagnote.application.catalog.detail.model.SystemTagDetail;
import com.tagnote.application.catalog.detail.model.TrackDetail;
import com.tagnote.application.catalog.importer.CatalogTrackReadService;
import com.tagnote.domain.enrichment.subject.SubjectType;
import com.tagnote.domain.resolution.ResolvedStatus;
import com.tagnote.infrastructure.persistence.resolution.SubjectTagResolvedJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TrackDetailReadService {

    private final CatalogTrackReadService catalogTrackReadService;
    private final SubjectTagResolvedJpaRepository resolvedRepository;

    public boolean hasResolvedProjection(long catalogTrackId) {
        return resolvedRepository.existsBySubjectTypeAndSubjectId(
                SubjectType.TRACK, catalogTrackId
        );
    }

    public TrackDetail getByCatalogTrackId(long catalogTrackId) {
        List<SystemTagDetail> systemTags = resolvedRepository.findVisibleBySubjectWithTag(
                        SubjectType.TRACK,
                        catalogTrackId,
                        ResolvedStatus.HIDDEN
                ).stream()
                .map(resolved -> new SystemTagDetail(
                        resolved.getTag().getTagId(),
                        resolved.getTag().getName(),
                        resolved.getScore()
                ))
                .toList();
        return new TrackDetail(
                catalogTrackReadService.getByCatalogId(catalogTrackId),
                systemTags
        );
    }
}
