package com.tagnote.application.catalog.detail.port;

import com.tagnote.application.catalog.detail.model.SpotifyAlbumDetail;
import com.tagnote.application.catalog.detail.model.SpotifyArtistDetail;

public interface SpotifyCatalogDetailProvider {
    SpotifyAlbumDetail getAlbum(String spotifyAlbumId);

    SpotifyArtistDetail getArtist(String spotifyArtistId);
}
