package com.tagstory.core.domain.tracks.service;

import com.tagstory.core.domain.tracks.service.dto.response.SearchTracksResponse;
import com.tagstory.core.domain.tracks.webclient.SpotifyWebClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import se.michaelthelin.spotify.model_objects.specification.AlbumSimplified;
import se.michaelthelin.spotify.model_objects.specification.ArtistSimplified;
import se.michaelthelin.spotify.model_objects.specification.Image;
import se.michaelthelin.spotify.model_objects.specification.Track;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TrackService {

    private final SpotifyWebClient spotifyWebClient;

    public List<SearchTracksResponse> search(String keyword, int page) {
        Track[] tracks = spotifyWebClient.getTrackInfoByKeyword(keyword, page);

        List<SearchTracksResponse> searchTracksResponseList = new ArrayList<>();
        for (Track track : tracks) {
            searchTracksResponseList.add(getTrackData(track));
        }
        return searchTracksResponseList;
    }

    private SearchTracksResponse getTrackData(Track track) {
        ArtistSimplified[] artists = track.getArtists();
        String artistName = artists[0].getName();

        AlbumSimplified album = track.getAlbum();
        String albumName = album.getName();

        Image[] images = album.getImages();
        String imageUrl = (images.length > 0) ? images[0].getUrl() : "NO_IMAGE";

        return SearchTracksResponse.onComplete(track.getId(), track.getName(), artistName, albumName, imageUrl);
    }
}
