package com.tagnote.application.catalog.detail;

import com.tagnote.application.catalog.detail.model.ArtistAlbumListing;
import com.tagnote.application.catalog.detail.model.SpotifyAlbumDetail;
import com.tagnote.application.catalog.detail.model.SpotifyAlbumTrack;
import com.tagnote.application.catalog.detail.model.SpotifyArtistDetail;
import com.tagnote.application.catalog.detail.model.SpotifyCatalogPage;
import com.tagnote.application.catalog.detail.port.SpotifyCatalogDetailProvider;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CatalogDetailService {
    private final SpotifyCatalogDetailProvider provider;
    private final ArtistAlbumIndexService artistAlbumIndexService;

    public SpotifyAlbumDetail getAlbum(String spotifyAlbumId) {
        return provider.getAlbum(spotifyAlbumId);
    }

    public SpotifyArtistDetail getArtist(String spotifyArtistId) {
        return provider.getArtist(spotifyArtistId);
    }

    public Optional<ArtistAlbumListing> getArtistAlbumListing(String spotifyArtistId, String releaseType, int page) {
        if (releaseType == null) {
            return Optional.of(ArtistAlbumListing.original(provider.getArtistAlbums(spotifyArtistId, page)));
        }
        return artistAlbumIndexService.read(spotifyArtistId, releaseType, page).map(ArtistAlbumListing::indexed);
    }

    public SpotifyCatalogPage<SpotifyAlbumTrack> getAlbumTracks(String spotifyAlbumId, int page) {
        return provider.getAlbumTracks(spotifyAlbumId, page);
    }
}
