package com.tagnote.infrastructure.external.spotify;

import com.tagnote.application.catalog.search.model.TrackSearchItem;
import com.tagnote.application.catalog.search.model.TrackSearchResult;
import com.tagnote.application.catalog.search.port.TrackSearchProvider;
import com.tagnote.infrastructure.external.spotify.dto.SpotifyJson.Album;
import com.tagnote.infrastructure.external.spotify.dto.SpotifyJson.Artist;
import com.tagnote.infrastructure.external.spotify.dto.SpotifyJson.Image;
import com.tagnote.infrastructure.external.spotify.dto.SpotifyJson.Page;
import com.tagnote.infrastructure.external.spotify.dto.SpotifyJson.Track;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SpotifyTrackSearchAdapter implements TrackSearchProvider {

    private final SpotifyHttpClient client;

    @Override
    public TrackSearchResult search(String keyword, int page) {
        Page<Track> trackInfo = client.searchTracks(keyword, page);
        List<TrackSearchItem> items = Arrays.stream(trackInfo.getItems())
                .map(this::toTrackSearchItem)
                .toList();

        return TrackSearchResult.of(items, trackInfo.getTotal());
    }

    private TrackSearchItem toTrackSearchItem(Track track) {
        Artist[] artists = track.getArtists();
        String artistName = artists[0].getName();

        Album album = track.getAlbum();
        String albumName = album.getName();

        Image[] images = album.getImages();
        String imageUrl = (images.length > 0) ? images[0].getUrl() : "NO_IMAGE";

        return TrackSearchItem.of(track.getId(), artistName, track.getName(), albumName, imageUrl);
    }
}
