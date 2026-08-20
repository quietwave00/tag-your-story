package com.tagnote.application.catalog.importer.port;

import com.tagnote.application.catalog.importer.model.SpotifyTrackMetadata;

public interface SpotifyTrackMetadataProvider {

    SpotifyTrackMetadata getTrack(String spotifyTrackId);
}
