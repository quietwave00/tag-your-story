package com.tagnote.application.enrichment;

import com.tagnote.application.resolution.TagResolutionService;
import com.tagnote.domain.enrichment.subject.SubjectRef;
import com.tagnote.infrastructure.persistence.catalog.TrackJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AlbumInheritanceRefreshService {
    private final TrackJpaRepository tracks;
    private final TagResolutionService resolver;

    public void refresh(long albumId, long selectedTrackId) {
        for (Long trackId : tracks.findIdsByAlbumId(albumId)) {
            if (trackId != selectedTrackId) resolver.resolve(SubjectRef.track(trackId));
        }
    }
}
