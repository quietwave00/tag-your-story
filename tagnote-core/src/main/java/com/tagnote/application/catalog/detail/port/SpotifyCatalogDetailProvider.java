package com.tagnote.application.catalog.detail.port;

import com.tagnote.application.catalog.detail.model.SpotifyAlbumDetail;
import com.tagnote.application.catalog.detail.model.SpotifyAlbumSummary;
import com.tagnote.application.catalog.detail.model.SpotifyAlbumTrack;
import com.tagnote.application.catalog.detail.model.SpotifyArtistDetail;
import com.tagnote.application.catalog.detail.model.SpotifyCatalogPage;

public interface SpotifyCatalogDetailProvider {
    SpotifyAlbumDetail getAlbum(String spotifyAlbumId);

    SpotifyArtistDetail getArtist(String spotifyArtistId);

    SpotifyCatalogPage<SpotifyAlbumSummary> getArtistAlbums(String spotifyArtistId, int page);

    SpotifyCatalogPage<SpotifyAlbumTrack> getAlbumTracks(String spotifyAlbumId, int page);
}
