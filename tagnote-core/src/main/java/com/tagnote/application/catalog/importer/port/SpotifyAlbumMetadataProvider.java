package com.tagnote.application.catalog.importer.port;

import com.tagnote.application.catalog.importer.model.SpotifyAlbumMetadata;

public interface SpotifyAlbumMetadataProvider {
    SpotifyAlbumMetadata getAlbum(String spotifyAlbumId);
}
