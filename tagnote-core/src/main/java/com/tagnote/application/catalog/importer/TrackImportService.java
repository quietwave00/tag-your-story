package com.tagnote.application.catalog.importer;

import com.tagnote.application.catalog.importer.model.ImportedTrack;
import com.tagnote.application.catalog.importer.model.SpotifyTrackMetadata;
import com.tagnote.application.catalog.importer.port.SpotifyTrackMetadataProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TrackImportService {

    private final CatalogTrackReadService catalogTrackReadService;
    private final CatalogWriteService catalogWriteService;
    private final SpotifyTrackMetadataProvider spotifyTrackMetadataProvider;

    public ImportedTrack importTrack(String spotifyTrackId) {
        ImportedTrack existingTrack = catalogTrackReadService.findBySpotifyId(spotifyTrackId).orElse(null);
        if (existingTrack != null) {
            return existingTrack;
        }

        SpotifyTrackMetadata metadata = spotifyTrackMetadataProvider.getTrack(spotifyTrackId);
        try {
            catalogWriteService.upsert(metadata);
        } catch (DataIntegrityViolationException firstConflict) {
            ImportedTrack concurrentlyImported = catalogTrackReadService
                    .findBySpotifyId(spotifyTrackId)
                    .orElse(null);
            if (concurrentlyImported != null) {
                return concurrentlyImported;
            }
            catalogWriteService.upsert(metadata);
        }
        return catalogTrackReadService.getBySpotifyId(spotifyTrackId);
    }
}
