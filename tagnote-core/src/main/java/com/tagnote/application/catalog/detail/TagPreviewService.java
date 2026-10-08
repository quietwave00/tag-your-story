package com.tagnote.application.catalog.detail;

import com.tagnote.application.catalog.detail.model.PreviewTagDetail;
import com.tagnote.application.enrichment.model.ObservationProcessingResult;
import com.tagnote.domain.enrichment.observation.ExternalTagObservationEntity;
import com.tagnote.domain.enrichment.observation.ExternalTagSource;
import com.tagnote.domain.enrichment.observation.ObservationStatus;
import com.tagnote.domain.enrichment.subject.SubjectType;
import com.tagnote.infrastructure.persistence.enrichment.ExternalTagObservationJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class TagPreviewService {
    private final ExternalTagObservationJpaRepository observations;

    public List<PreviewTagDetail> fromProcessed(
            ObservationProcessingResult album, ObservationProcessingResult track) {
        return select(Stream.concat(album.newObservations().stream(), track.newObservations().stream())
                .sorted(Comparator.comparingInt(row -> priority(row.source())))
                .map(row -> new PreviewCandidate(row.rawName(), row.source(), row.normalizedName())));
    }

    @Transactional(readOnly = true)
    public List<PreviewTagDetail> fromPersisted(long trackId, long albumId) {
        return select(observations.findForPreview(trackId, albumId, ObservationStatus.NEW).stream()
                .sorted(Comparator.comparingInt((ExternalTagObservationEntity row) -> priority(row.getSource()))
                        .thenComparing(ExternalTagObservationEntity::getObservedAt)
                        .thenComparing(ExternalTagObservationEntity::getObservationId))
                .map(row -> new PreviewCandidate(row.getRawName(), row.getSource(), row.getNormalizedName())));
    }

    @Transactional(readOnly = true)
    public List<PreviewTagDetail> fromPersistedAlbum(long albumId) {
        return select(observations.findBySubjectTypeAndSubjectIdAndStatus(
                        SubjectType.ALBUM, albumId, ObservationStatus.NEW).stream()
                .sorted(Comparator.comparingInt((ExternalTagObservationEntity row) -> priority(row.getSource()))
                        .thenComparing(ExternalTagObservationEntity::getObservedAt)
                        .thenComparing(ExternalTagObservationEntity::getObservationId))
                .map(row -> new PreviewCandidate(row.getRawName(), row.getSource(), row.getNormalizedName())));
    }

    private List<PreviewTagDetail> select(Stream<PreviewCandidate> candidates) {
        Map<String, PreviewTagDetail> unique = new LinkedHashMap<>();
        candidates.forEach(candidate -> unique.putIfAbsent(candidate.normalizedName(),
                new PreviewTagDetail(candidate.name(), candidate.source())));
        return unique.values().stream().limit(5).toList();
    }

    private int priority(ExternalTagSource source) {
        return switch (source) {
            case MUSICBRAINZ -> 0;
            case DISCOGS -> 1;
            case LASTFM -> 2;
        };
    }

    private record PreviewCandidate(String name, ExternalTagSource source, String normalizedName) {}
}
