package com.tagnote.application.catalog.importer;

import com.tagnote.application.catalog.importer.model.ImportedAlbum;
import com.tagnote.application.catalog.importer.model.ImportedArtist;
import com.tagnote.infrastructure.persistence.catalog.AlbumArtistJpaRepository;
import com.tagnote.infrastructure.persistence.catalog.AlbumJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CatalogAlbumReadService {

    private final AlbumJpaRepository albums;
    private final AlbumArtistJpaRepository credits;

    public Optional<ImportedAlbum> findBySpotifyId(String spotifyId) {
        return albums.findBySpotifyId(spotifyId).map(album -> ImportedAlbum.of(
                album.getAlbumId(), album.getSpotifyId(), album.getTitle(), album.getReleaseYear(),
                credits.findAllByAlbumAlbumIdOrderByPositionAsc(album.getAlbumId()).stream()
                        .map(credit -> ImportedArtist.of(
                                credit.getArtist().getArtistId(), credit.getArtist().getSpotifyId(),
                                credit.getArtist().getName(), credit.getPosition()))
                        .toList()));
    }
}
