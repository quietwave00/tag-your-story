package com.tagstory.core.domain.tracks.service;

import com.tagstory.core.domain.tracks.service.dto.TrackData;
import com.tagstory.core.domain.tracks.service.dto.response.SearchTrackList;
import com.tagstory.core.domain.tracks.webclient.SpotifyWebClient;
import com.tagstory.core.domain.tracks.webclient.dto.TrackInfo;
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

    public SearchTrackList search(String keyword, int page) {
        TrackInfo trackInfo = spotifyWebClient.getTrackInfoByKeyword(keyword, page);
        List<TrackData> trackDataList = Arrays.stream(trackInfo.getTracks())
                .map(this::getTrackData)
                .collect(Collectors.toList());

        return SearchTrackList.onComplete(trackDataList, trackInfo.getTotalCount());
    }

    public TrackData getDetail(String trackId) {
        Track track = spotifyWebClient.getDetailTrackInfo(trackId);
        return getTrackData(track);
    }

    private TrackData getTrackData(Track track) {
        ArtistSimplified[] artists = track.getArtists();
        String artistName = artists[0].getName();

        AlbumSimplified album = track.getAlbum();
        String albumName = album.getName();

        Image[] images = album.getImages();
        String imageUrl = (images.length > 0) ? images[0].getUrl() : "NO_IMAGE";

        return TrackData.of(track.getId(), artistName, track.getName(), albumName, imageUrl);
    }
}
