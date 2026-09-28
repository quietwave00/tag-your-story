package com.tagnote.application.catalog.importer;

import com.tagnote.application.catalog.importer.model.ImportedAlbum;
import com.tagnote.application.catalog.importer.model.ImportedArtist;
import com.tagnote.application.catalog.importer.model.ImportedTrack;
import com.tagnote.application.catalog.importer.model.SpotifyArtistMetadata;
import com.tagnote.application.catalog.importer.model.SpotifyTrackMetadata;
import com.tagnote.application.catalog.importer.port.CatalogConflictTranslator;
import com.tagnote.domain.catalog.album.AlbumArtistEntity;
import com.tagnote.domain.catalog.album.AlbumEntity;
import com.tagnote.domain.catalog.artist.ArtistEntity;
import com.tagnote.domain.catalog.track.TrackArtistEntity;
import com.tagnote.domain.catalog.track.TrackEntity;
import com.tagnote.infrastructure.persistence.catalog.AlbumArtistJpaRepository;
import com.tagnote.infrastructure.persistence.catalog.AlbumJpaRepository;
import com.tagnote.infrastructure.persistence.catalog.ArtistJpaRepository;
import com.tagnote.infrastructure.persistence.catalog.TrackArtistJpaRepository;
import com.tagnote.infrastructure.persistence.catalog.TrackJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class CatalogWriteService {

    private final ArtistJpaRepository artistRepository;
    private final AlbumJpaRepository albumRepository;
    private final TrackJpaRepository trackRepository;
    private final AlbumArtistJpaRepository albumArtistRepository;
    private final TrackArtistJpaRepository trackArtistRepository;
    private final CatalogConflictTranslator conflictTranslator;

    @Transactional
    public ImportedTrack create(SpotifyTrackMetadata metadata) {
        try {
            return createWithinTransaction(metadata);
        } catch (DataIntegrityViolationException failure) {
            throw conflictTranslator.translate(failure);
        }
    }

    private ImportedTrack createWithinTransaction(SpotifyTrackMetadata metadata) {
        AlbumEntity album = albumRepository.findBySpotifyId(metadata.getSpotifyAlbumId()).orElse(null);
        boolean createAlbum = album == null;
        Map<String, SpotifyArtistMetadata> artistMetadataBySpotifyId = requiredArtists(metadata, createAlbum);
        Map<String, ArtistEntity> artistsBySpotifyId = findOrCreateArtists(artistMetadataBySpotifyId);

        List<ImportedArtist> albumArtists;
        if (createAlbum) {
            album = albumRepository.saveAndFlush(AlbumEntity.create(
                    metadata.getAlbumTitle(),
                    metadata.getSpotifyAlbumId(),
                    metadata.getReleaseYear()
            ));
            AlbumEntity savedAlbum = album;
            albumArtistRepository.saveAll(metadata.getAlbumArtists().stream()
                    .map(artist -> AlbumArtistEntity.create(
                            savedAlbum,
                            artistsBySpotifyId.get(artist.getSpotifyArtistId()),
                            artist.getPosition()
                    ))
                    .toList());
            albumArtists = toImportedArtists(metadata.getAlbumArtists(), artistsBySpotifyId);
        } else {
            albumArtists = albumArtistRepository
                    .findAllByAlbumAlbumIdOrderByPositionAsc(album.getAlbumId())
                    .stream()
                    .map(credit -> toImportedArtist(credit.getArtist(), credit.getPosition()))
                    .toList();
        }

        TrackEntity track = trackRepository.saveAndFlush(TrackEntity.create(
                metadata.getTitle(),
                metadata.getSpotifyTrackId(),
                metadata.getIsrc(),
                metadata.getDurationMs(),
                album
        ));
        trackArtistRepository.saveAll(metadata.getTrackArtists().stream()
                .map(artist -> TrackArtistEntity.create(
                        track,
                        artistsBySpotifyId.get(artist.getSpotifyArtistId()),
                        artist.getPosition()
                ))
                .toList());
        trackArtistRepository.flush();

        ImportedAlbum importedAlbum = ImportedAlbum.of(
                album.getAlbumId(),
                album.getSpotifyId(),
                album.getTitle(),
                album.getReleaseYear(),
                albumArtists
        );
        ImportedTrack importedTrack = ImportedTrack.of(
                track.getTrackId(),
                track.getSpotifyId(),
                track.getMusicbrainzId(),
                track.getTitle(),
                track.getIsrc(),
                track.getDurationMs(),
                toImportedArtists(metadata.getTrackArtists(), artistsBySpotifyId),
                importedAlbum
        );
        return importedTrack;
    }

    private Map<String, SpotifyArtistMetadata> requiredArtists(
            SpotifyTrackMetadata metadata,
            boolean createAlbum
    ) {
        Stream<SpotifyArtistMetadata> albumArtists = createAlbum
                ? metadata.getAlbumArtists().stream()
                : Stream.empty();
        return Stream.concat(metadata.getTrackArtists().stream(), albumArtists)
                .collect(Collectors.toMap(
                        SpotifyArtistMetadata::getSpotifyArtistId,
                        Function.identity(),
                        (first, ignored) -> first,
                        LinkedHashMap::new
                ));
    }

    private Map<String, ArtistEntity> findOrCreateArtists(
            Map<String, SpotifyArtistMetadata> metadataBySpotifyId
    ) {
        Map<String, ArtistEntity> artistsBySpotifyId = artistRepository
                .findAllBySpotifyIdIn(metadataBySpotifyId.keySet())
                .stream()
                .collect(Collectors.toMap(ArtistEntity::getSpotifyId, Function.identity()));

        Set<String> existingIds = artistsBySpotifyId.keySet();
        List<ArtistEntity> missingArtists = metadataBySpotifyId.values().stream()
                .filter(metadata -> !existingIds.contains(metadata.getSpotifyArtistId()))
                .map(metadata -> ArtistEntity.create(metadata.getName(), metadata.getSpotifyArtistId()))
                .toList();
        artistRepository.saveAllAndFlush(missingArtists).forEach(
                artist -> artistsBySpotifyId.put(artist.getSpotifyId(), artist)
        );
        return artistsBySpotifyId;
    }

    private List<ImportedArtist> toImportedArtists(
            List<SpotifyArtistMetadata> credits,
            Map<String, ArtistEntity> artistsBySpotifyId
    ) {
        return credits.stream()
                .map(credit -> toImportedArtist(
                        artistsBySpotifyId.get(credit.getSpotifyArtistId()),
                        credit.getPosition()
                ))
                .toList();
    }

    private ImportedArtist toImportedArtist(ArtistEntity artist, int position) {
        return ImportedArtist.of(artist.getArtistId(), artist.getSpotifyId(), artist.getName(), position);
    }
}
