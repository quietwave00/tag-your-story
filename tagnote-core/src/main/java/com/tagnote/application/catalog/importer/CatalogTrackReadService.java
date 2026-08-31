package com.tagnote.application.catalog.importer;

import com.tagnote.application.catalog.importer.model.ImportedAlbum;
import com.tagnote.application.catalog.importer.model.ImportedArtist;
import com.tagnote.application.catalog.importer.model.ImportedTrack;
import com.tagnote.domain.catalog.album.AlbumArtistEntity;
import com.tagnote.domain.catalog.album.AlbumEntity;
import com.tagnote.domain.catalog.track.TrackArtistEntity;
import com.tagnote.domain.catalog.track.TrackEntity;
import com.tagnote.infrastructure.persistence.catalog.AlbumArtistJpaRepository;
import com.tagnote.infrastructure.persistence.catalog.TrackArtistJpaRepository;
import com.tagnote.infrastructure.persistence.catalog.TrackJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CatalogTrackReadService {

    private final TrackJpaRepository trackRepository;
    private final TrackArtistJpaRepository trackArtistRepository;
    private final AlbumArtistJpaRepository albumArtistRepository;

    public Optional<ImportedTrack> findBySpotifyId(String spotifyTrackId) {
        return trackRepository.findBySpotifyId(spotifyTrackId).map(this::toImportedTrack);
    }

    public ImportedTrack getBySpotifyId(String spotifyTrackId) {
        return findBySpotifyId(spotifyTrackId)
                .orElseThrow(() -> new IllegalStateException("Imported track was not found: " + spotifyTrackId));
    }

    public ImportedTrack getByCatalogId(long catalogTrackId) {
        TrackEntity track = trackRepository.findByIdWithAlbum(catalogTrackId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Catalog track was not found: " + catalogTrackId
                ));
        return toImportedTrack(track);
    }

    private ImportedTrack toImportedTrack(TrackEntity track) {
        AlbumEntity album = track.getAlbum();
        List<ImportedArtist> trackArtists = trackArtistRepository
                .findAllByTrackTrackIdOrderByPositionAsc(track.getTrackId())
                .stream()
                .map(this::toImportedArtist)
                .toList();
        List<ImportedArtist> albumArtists = albumArtistRepository
                .findAllByAlbumAlbumIdOrderByPositionAsc(album.getAlbumId())
                .stream()
                .map(this::toImportedArtist)
                .toList();

        ImportedAlbum importedAlbum = ImportedAlbum.of(
                album.getAlbumId(),
                album.getSpotifyId(),
                album.getTitle(),
                album.getReleaseYear(),
                albumArtists
        );
        return ImportedTrack.of(
                track.getTrackId(),
                track.getSpotifyId(),
                track.getTitle(),
                track.getIsrc(),
                track.getDurationMs(),
                trackArtists,
                importedAlbum
        );
    }

    private ImportedArtist toImportedArtist(TrackArtistEntity credit) {
        return ImportedArtist.of(
                credit.getArtist().getArtistId(),
                credit.getArtist().getSpotifyId(),
                credit.getArtist().getName(),
                credit.getPosition()
        );
    }

    private ImportedArtist toImportedArtist(AlbumArtistEntity credit) {
        return ImportedArtist.of(
                credit.getArtist().getArtistId(),
                credit.getArtist().getSpotifyId(),
                credit.getArtist().getName(),
                credit.getPosition()
        );
    }
}
