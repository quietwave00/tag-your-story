package com.tagnote.infrastructure.external.spotify;

import com.tagnote.application.catalog.search.model.TrackSearchItem;
import com.tagnote.application.catalog.search.model.TrackSearchResult;
import com.tagnote.application.catalog.search.port.TrackSearchProvider;
import com.tagnote.core.domain.tracks.webclient.SpotifyWebClient;
import com.tagnote.core.domain.tracks.webclient.dto.TrackInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import se.michaelthelin.spotify.model_objects.specification.AlbumSimplified;
import se.michaelthelin.spotify.model_objects.specification.ArtistSimplified;
import se.michaelthelin.spotify.model_objects.specification.Image;
import se.michaelthelin.spotify.model_objects.specification.Track;

import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
public class SpotifyTrackSearchAdapter implements TrackSearchProvider {

    private final SpotifyWebClient spotifyWebClient;

    @Override
    public TrackSearchResult search(String keyword, int page) {
        TrackInfo trackInfo = spotifyWebClient.getTrackInfoByKeyword(keyword, page);
        List<TrackSearchItem> items = Arrays.stream(trackInfo.getTracks())
                .map(this::toTrackSearchItem)
                .toList();

        return TrackSearchResult.of(items, trackInfo.getTotalCount());
    }

    private TrackSearchItem toTrackSearchItem(Track track) {
        ArtistSimplified[] artists = track.getArtists();
        String artistName = artists[0].getName();

        AlbumSimplified album = track.getAlbum();
        String albumName = album.getName();

        Image[] images = album.getImages();
        String imageUrl = (images.length > 0) ? images[0].getUrl() : "NO_IMAGE";

        return TrackSearchItem.of(track.getId(), artistName, track.getName(), albumName, imageUrl);
    }
}
