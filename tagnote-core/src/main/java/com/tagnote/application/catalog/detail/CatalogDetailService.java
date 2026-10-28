package com.tagnote.application.catalog.detail;

import com.tagnote.application.catalog.detail.model.SpotifyAlbumDetail;
import com.tagnote.application.catalog.detail.model.SpotifyAlbumSummary;
import com.tagnote.application.catalog.detail.model.SpotifyAlbumTrack;
import com.tagnote.application.catalog.detail.model.SpotifyArtistDetail;
import com.tagnote.application.catalog.detail.model.SpotifyCatalogPage;
import com.tagnote.application.catalog.detail.port.SpotifyCatalogDetailProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CatalogDetailService {
    private final SpotifyCatalogDetailProvider provider;

    public SpotifyAlbumDetail getAlbum(String spotifyAlbumId) {
        return provider.getAlbum(spotifyAlbumId);
    }

    public SpotifyArtistDetail getArtist(String spotifyArtistId) {
        return provider.getArtist(spotifyArtistId);
    }

    public SpotifyCatalogPage<SpotifyAlbumSummary> getArtistAlbums(String spotifyArtistId, int page) {
        return provider.getArtistAlbums(spotifyArtistId, page);
    }

    public SpotifyCatalogPage<SpotifyAlbumTrack> getAlbumTracks(String spotifyAlbumId, int page) {
        return provider.getAlbumTracks(spotifyAlbumId, page);
    }
}
