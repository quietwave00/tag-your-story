package com.tagnote.infrastructure.external.spotify;

import com.tagnote.application.catalog.importer.model.SpotifyArtistMetadata;
import com.tagnote.application.catalog.importer.model.SpotifyTrackMetadata;
import com.tagnote.application.catalog.importer.port.SpotifyTrackMetadataProvider;
import com.tagnote.core.domain.tracks.webclient.SpotifyWebClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import se.michaelthelin.spotify.model_objects.specification.AlbumSimplified;
import se.michaelthelin.spotify.model_objects.specification.ArtistSimplified;
import se.michaelthelin.spotify.model_objects.specification.Track;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class SpotifyTrackMetadataAdapter implements SpotifyTrackMetadataProvider {

    private final SpotifyWebClient spotifyWebClient;

    @Override
    public SpotifyTrackMetadata getTrack(String spotifyTrackId) {
        Track track = spotifyWebClient.getDetailTrackInfo(spotifyTrackId);
        AlbumSimplified album = requireAlbum(track.getAlbum());

        return SpotifyTrackMetadata.of(
                requireText(track.getId(), "Spotify track id"),
                requireText(track.getName(), "Spotify track title"),
                isrc(track),
                requireDuration(track.getDurationMs()),
                artists(track.getArtists(), "track"),
                requireText(album.getId(), "Spotify album id"),
                requireText(album.getName(), "Spotify album title"),
                releaseYear(album.getReleaseDate()),
                artists(album.getArtists(), "album")
        );
    }

    private List<SpotifyArtistMetadata> artists(ArtistSimplified[] spotifyArtists, String subject) {
        if (spotifyArtists == null || spotifyArtists.length == 0) {
            throw new IllegalArgumentException("Spotify " + subject + " artists must not be empty");
        }

        Map<String, String> uniqueArtists = new LinkedHashMap<>();
        for (ArtistSimplified artist : spotifyArtists) {
            if (artist == null) {
                throw new IllegalArgumentException("Spotify " + subject + " artist must not be null");
            }
            uniqueArtists.putIfAbsent(
                    requireText(artist.getId(), "Spotify artist id"),
                    requireText(artist.getName(), "Spotify artist name")
            );
        }

        List<SpotifyArtistMetadata> result = new ArrayList<>(uniqueArtists.size());
        int position = 0;
        for (Map.Entry<String, String> artist : uniqueArtists.entrySet()) {
            result.add(SpotifyArtistMetadata.of(artist.getKey(), artist.getValue(), position++));
        }
        return result;
    }

    private String isrc(Track track) {
        if (track.getExternalIds() == null || track.getExternalIds().getExternalIds() == null) {
            return null;
        }
        return track.getExternalIds().getExternalIds().get("isrc");
    }

    private Integer releaseYear(String releaseDate) {
        if (releaseDate == null || releaseDate.length() < 4) {
            return null;
        }
        String year = releaseDate.substring(0, 4);
        if (!year.chars().allMatch(Character::isDigit)) {
            return null;
        }
        return Integer.valueOf(year);
    }

    private AlbumSimplified requireAlbum(AlbumSimplified album) {
        if (album == null) {
            throw new IllegalArgumentException("Spotify album must not be null");
        }
        return album;
    }

    private Integer requireDuration(Integer durationMs) {
        if (durationMs == null || durationMs < 0) {
            throw new IllegalArgumentException("Spotify track duration must not be negative");
        }
        return durationMs;
    }

    private String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
