package com.tagnote.application.catalog.detail;

import com.tagnote.application.catalog.detail.model.AlbumDetail;
import com.tagnote.application.catalog.detail.model.SystemTagDetail;
import com.tagnote.application.catalog.importer.model.ImportedAlbum;
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
public class AlbumDetailReadService {
    private final SubjectTagResolvedJpaRepository resolvedRepository;

    public Optional<AlbumDetail> findResolved(ImportedAlbum album) {
        List<SubjectTagResolvedEntity> resolved = resolvedRepository.findAllBySubjectWithTag(
                SubjectType.ALBUM, album.getAlbumId());
        if (resolved.isEmpty()) return Optional.empty();
        return Optional.of(new AlbumDetail(album, resolved.stream()
                .filter(row -> row.getStatus() != ResolvedStatus.HIDDEN)
                .sorted(Comparator.comparingDouble(SubjectTagResolvedEntity::getScore).reversed()
                        .thenComparingLong(row -> row.getTag().getTagId()))
                .map(row -> new SystemTagDetail(row.getTag().getTagId(), row.getTag().getName(), row.getScore()))
                .toList()));
    }

    public AlbumDetail fromResolved(ImportedAlbum album, List<ResolvedTagResult> resolved) {
        return new AlbumDetail(album, resolved.stream()
                .filter(row -> row.status() != ResolvedStatus.HIDDEN)
                .map(row -> new SystemTagDetail(row.tagId(), row.tagName(), row.score()))
                .toList());
    }
}
