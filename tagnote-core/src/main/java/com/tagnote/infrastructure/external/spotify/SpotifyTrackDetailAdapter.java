package com.tagnote.infrastructure.external.spotify;

import com.tagnote.application.catalog.detail.port.TrackDetailProvider;
import com.tagnote.core.domain.tracks.service.dto.TrackData;
import com.tagnote.infrastructure.external.spotify.dto.SpotifyJson.Album;
import com.tagnote.infrastructure.external.spotify.dto.SpotifyJson.Artist;
import com.tagnote.infrastructure.external.spotify.dto.SpotifyJson.Image;
import com.tagnote.infrastructure.external.spotify.dto.SpotifyJson.Track;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SpotifyTrackDetailAdapter implements TrackDetailProvider {
    private final SpotifyHttpClient client;

    @Override
    public TrackData getTrack(String spotifyTrackId) {
        Track track = client.track(spotifyTrackId);
        Artist artist = track.getArtists()[0];
        Album album = track.getAlbum();
        Image[] images = album.getImages();
        String imageUrl = images.length > 0 ? images[0].getUrl() : "NO_IMAGE";
        return TrackData.of(track.getId(), artist.getName(), track.getName(), album.getName(), imageUrl,
                artist.getId(), album.getId());
    }
}
