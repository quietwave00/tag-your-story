package com.tagnote.application.catalog.importer;

import com.tagnote.application.catalog.importer.model.ImportedAlbum;
import com.tagnote.application.catalog.importer.model.ImportedArtist;
import com.tagnote.application.catalog.importer.model.SpotifyAlbumMetadata;
import com.tagnote.application.catalog.importer.port.CatalogConflictTranslator;
import com.tagnote.domain.catalog.album.AlbumArtistEntity;
import com.tagnote.domain.catalog.album.AlbumEntity;
import com.tagnote.domain.catalog.artist.ArtistEntity;
import com.tagnote.infrastructure.persistence.catalog.AlbumArtistJpaRepository;
import com.tagnote.infrastructure.persistence.catalog.AlbumJpaRepository;
import com.tagnote.infrastructure.persistence.catalog.ArtistJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CatalogAlbumWriteService {

    private final ArtistJpaRepository artists;
    private final AlbumJpaRepository albums;
    private final AlbumArtistJpaRepository credits;
    private final CatalogConflictTranslator conflictTranslator;

    @Transactional
    public ImportedAlbum create(SpotifyAlbumMetadata metadata) {
        try {
            Map<String, ArtistEntity> bySpotifyId = artists.findAllBySpotifyIdIn(
                    metadata.artists().stream().map(artist -> artist.getSpotifyArtistId()).toList()
            ).stream().collect(Collectors.toMap(ArtistEntity::getSpotifyId, Function.identity()));
            metadata.artists().stream()
                    .filter(artist -> !bySpotifyId.containsKey(artist.getSpotifyArtistId()))
                    .map(artist -> ArtistEntity.create(artist.getName(), artist.getSpotifyArtistId()))
                    .forEach(artist -> {
                        ArtistEntity saved = artists.saveAndFlush(artist);
                        bySpotifyId.put(saved.getSpotifyId(), saved);
                    });
            AlbumEntity album = albums.saveAndFlush(AlbumEntity.create(
                    metadata.title(), metadata.spotifyId(), metadata.releaseYear()));
            credits.saveAll(metadata.artists().stream()
                    .map(artist -> AlbumArtistEntity.create(album,
                            bySpotifyId.get(artist.getSpotifyArtistId()), artist.getPosition()))
                    .toList());
            credits.flush();
            return ImportedAlbum.of(album.getAlbumId(), album.getSpotifyId(), album.getTitle(),
                    album.getReleaseYear(), metadata.artists().stream()
                            .map(artist -> {
                                ArtistEntity saved = bySpotifyId.get(artist.getSpotifyArtistId());
                                return ImportedArtist.of(saved.getArtistId(), saved.getSpotifyId(),
                                        saved.getName(), artist.getPosition());
                            }).toList());
        } catch (DataIntegrityViolationException failure) {
            throw conflictTranslator.translate(failure);
        }
    }
}
