package com.tagnote.application.catalog.importer;

import com.tagnote.application.enrichment.model.CatalogExternalIdentityMatch;
import com.tagnote.domain.catalog.album.AlbumEntity;
import com.tagnote.domain.catalog.track.TrackEntity;
import com.tagnote.infrastructure.persistence.catalog.AlbumJpaRepository;
import com.tagnote.infrastructure.persistence.catalog.TrackJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CatalogExternalIdentityWriteService {

    private final TrackJpaRepository trackRepository;
    private final AlbumJpaRepository albumRepository;

    @Transactional
    public void attach(long catalogTrackId, CatalogExternalIdentityMatch match) {
        if (match == null || match.isEmpty()) {
            return;
        }
        TrackEntity track = trackRepository.findByIdWithAlbumForUpdate(catalogTrackId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Catalog track was not found: " + catalogTrackId
                ));
        if (match.musicBrainzRecordingId() != null) {
            track.attachMusicBrainzRecordingId(match.musicBrainzRecordingId());
        }
        if (match.musicBrainzReleaseGroupId() != null) {
            AlbumEntity album = albumRepository.findByIdForUpdate(track.getAlbum().getAlbumId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Catalog album was not found: " + track.getAlbum().getAlbumId()
                    ));
            album.attachMusicBrainzReleaseGroupId(match.musicBrainzReleaseGroupId());
        }
    }
}
