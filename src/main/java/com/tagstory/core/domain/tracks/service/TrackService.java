package com.tagstory.core.domain.tracks.service;

import com.tagstory.core.domain.tracks.service.dto.response.SearchTracksResponse;
import com.tagstory.core.domain.tracks.util.SpotifyWebClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.core5.http.ParseException;
import org.springframework.stereotype.Service;
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.exceptions.SpotifyWebApiException;
import se.michaelthelin.spotify.model_objects.specification.*;
import se.michaelthelin.spotify.requests.data.search.simplified.SearchTracksRequest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TrackService {

    private final SpotifyWebClient spotifyWebClient;

    public List<SearchTracksResponse> search(String keyword, int page) {
        List<SearchTracksResponse> searchTracksResponseList = new ArrayList<>();
        try {
            SpotifyApi spotifyApi = spotifyWebClient.getSpotifyApi();
            SearchTracksRequest searchTrackRequest = spotifyApi.searchTracks(keyword)
                    .limit(10)
                    .offset(page)
                    .build();
            Paging<Track> searchResult = searchTrackRequest.execute();
            Track[] tracks = searchResult.getItems();

            for (Track track : tracks) {
                searchTracksResponseList.add(getTrackData(track));
            }
        } catch(IOException | SpotifyWebApiException | ParseException e) {
            log.error(e.getMessage());
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
