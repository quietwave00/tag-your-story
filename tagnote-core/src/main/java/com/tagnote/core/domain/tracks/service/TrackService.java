package com.tagnote.core.domain.tracks.service;

import com.tagnote.core.domain.tracks.service.dto.TrackData;
import com.tagnote.core.domain.tracks.service.dto.response.RankingList;
import com.tagnote.core.domain.tracks.util.SearchKeywordTracker;
import com.tagnote.core.domain.tracks.webclient.SpotifyWebClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.michaelthelin.spotify.model_objects.specification.AlbumSimplified;
import se.michaelthelin.spotify.model_objects.specification.ArtistSimplified;
import se.michaelthelin.spotify.model_objects.specification.Image;
import se.michaelthelin.spotify.model_objects.specification.Track;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class TrackService {

    private final SpotifyWebClient spotifyWebClient;
    private final SearchKeywordTracker tracker;

    public TrackData getDetail(String trackId) {
        Track track = spotifyWebClient.getDetailTrackInfo(trackId);
        return getTrackData(track);
    }

    public RankingList getKeywordRanking() {
        return RankingList.onComplete(tracker.getTopSearchKeywordList());
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
