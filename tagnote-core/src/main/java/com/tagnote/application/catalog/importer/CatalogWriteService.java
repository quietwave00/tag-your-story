package com.tagnote.application.catalog.importer;

import com.tagnote.application.catalog.importer.model.SpotifyArtistMetadata;
import com.tagnote.application.catalog.importer.model.SpotifyTrackMetadata;
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

    @Transactional
    public void upsert(SpotifyTrackMetadata metadata) {
        if (trackRepository.findBySpotifyId(metadata.getSpotifyTrackId()).isPresent()) {
            return;
        }

        Map<String, SpotifyArtistMetadata> artistMetadataBySpotifyId = allArtists(metadata);
        Map<String, ArtistEntity> artistsBySpotifyId = findOrCreateArtists(artistMetadataBySpotifyId);

        AlbumEntity album = albumRepository.findBySpotifyId(metadata.getSpotifyAlbumId()).orElse(null);
        if (album == null) {
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
    }

    private Map<String, SpotifyArtistMetadata> allArtists(SpotifyTrackMetadata metadata) {
        return Stream.concat(metadata.getTrackArtists().stream(), metadata.getAlbumArtists().stream())
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
}
