package com.tagstory.core.domain.tracks.service;

import com.tagstory.core.domain.tracks.service.dto.response.DetailTrack;
import com.tagstory.core.domain.tracks.service.dto.response.SearchTracks;
import com.tagstory.core.domain.tracks.webclient.SpotifyWebClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import se.michaelthelin.spotify.model_objects.specification.AlbumSimplified;
import se.michaelthelin.spotify.model_objects.specification.ArtistSimplified;
import se.michaelthelin.spotify.model_objects.specification.Image;
import se.michaelthelin.spotify.model_objects.specification.Track;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TrackService {

    private final SpotifyWebClient spotifyWebClient;

    public List<SearchTracks> search(String keyword, int page) {
        Track[] tracks = spotifyWebClient.getTrackInfoByKeyword(keyword, page);
        return Arrays.stream(tracks)
                .map(track -> getTrackData(track, SearchTracks::onComplete))
                .collect(Collectors.toList());
    }

    public DetailTrack getDetail(String trackId) {
        Track track = spotifyWebClient.getDetailTrackInfo(trackId);
        return getTrackData(track, DetailTrack::onComplete);
    }

    private <T> T getTrackData(Track track, TrackDataConverter<T> converter) {
        ArtistSimplified[] artists = track.getArtists();
        String artistName = artists[0].getName();

        AlbumSimplified album = track.getAlbum();
        String albumName = album.getName();

        Image[] images = album.getImages();
        String imageUrl = (images.length > 0) ? images[0].getUrl() : "NO_IMAGE";

        return converter.convert(track.getId(), artistName, track.getName(), albumName, imageUrl);
    }

    @FunctionalInterface
    interface TrackDataConverter<T> {
        T convert(String trackId, String artistName, String title, String albumName, String imageUrl);
    }
}
