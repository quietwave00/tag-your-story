package com.tagnote.application.catalog.importer;

import com.tagnote.application.catalog.importer.exception.CatalogDuplicateException;
import com.tagnote.application.catalog.importer.model.ImportedAlbum;
import com.tagnote.application.catalog.importer.model.SpotifyAlbumMetadata;
import com.tagnote.application.catalog.importer.port.SpotifyAlbumMetadataProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AlbumImportService {

    private final CatalogAlbumReadService reader;
    private final CatalogAlbumWriteService writer;
    private final SpotifyAlbumMetadataProvider spotify;

    public ImportedAlbum importAlbum(String spotifyAlbumId) {
        ImportedAlbum existing = reader.findBySpotifyId(spotifyAlbumId).orElse(null);
        if (existing != null) return existing;
        SpotifyAlbumMetadata metadata = spotify.getAlbum(spotifyAlbumId);
        try {
            return writer.create(metadata);
        } catch (CatalogDuplicateException conflict) {
            ImportedAlbum concurrent = reader.findBySpotifyId(spotifyAlbumId).orElse(null);
            if (concurrent != null) return concurrent;
            if (conflict.isParentConflict()) return writer.create(metadata);
            throw conflict;
        }
    }
}
