package com.tagnote.infrastructure.external.spotify;

import com.tagnote.application.catalog.importer.model.SpotifyAlbumMetadata;
import com.tagnote.application.catalog.importer.model.SpotifyArtistMetadata;
import com.tagnote.application.catalog.importer.port.SpotifyAlbumMetadataProvider;
import com.tagnote.core.domain.tracks.webclient.SpotifyWebClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import se.michaelthelin.spotify.model_objects.specification.Album;
import se.michaelthelin.spotify.model_objects.specification.ArtistSimplified;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class SpotifyAlbumMetadataAdapter implements SpotifyAlbumMetadataProvider {

    private final SpotifyWebClient client;

    @Override
    public SpotifyAlbumMetadata getAlbum(String spotifyAlbumId) {
        Album album = client.getDetailAlbumInfo(spotifyAlbumId);
        if (album.getId() == null || album.getId().isBlank()
                || album.getName() == null || album.getName().isBlank()) {
            throw new IllegalArgumentException("Spotify Album identity is missing");
        }
        ArtistSimplified[] credits = album.getArtists();
        if (credits == null || credits.length == 0) {
            throw new IllegalArgumentException("Spotify Album artists are missing");
        }
        Map<String, String> unique = new LinkedHashMap<>();
        for (ArtistSimplified artist : credits) {
            if (artist == null || artist.getId() == null || artist.getId().isBlank()
                    || artist.getName() == null || artist.getName().isBlank()) {
                throw new IllegalArgumentException("Spotify Album artist identity is missing");
            }
            unique.putIfAbsent(artist.getId(), artist.getName());
        }
        List<SpotifyArtistMetadata> artists = new ArrayList<>();
        unique.forEach((id, name) -> artists.add(SpotifyArtistMetadata.of(id, name, artists.size())));
        String releaseDate = album.getReleaseDate();
        Integer releaseYear = releaseDate != null && releaseDate.length() >= 4
                && releaseDate.substring(0, 4).chars().allMatch(Character::isDigit)
                ? Integer.valueOf(releaseDate.substring(0, 4)) : null;
        return new SpotifyAlbumMetadata(album.getId(), album.getName(), releaseYear, artists);
    }
}
