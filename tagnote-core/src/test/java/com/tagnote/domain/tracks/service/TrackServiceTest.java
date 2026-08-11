package com.tagnote.domain.tracks.service;

import com.tagnote.core.domain.tracks.service.TrackService;
import com.tagnote.core.domain.tracks.service.dto.TrackData;
import com.tagnote.core.domain.tracks.service.dto.response.RankingList;
import com.tagnote.core.domain.tracks.service.dto.response.SearchTrackList;
import com.tagnote.core.domain.tracks.util.SearchKeywordTracker;
import com.tagnote.core.domain.tracks.webclient.SpotifyWebClient;
import com.tagnote.core.domain.tracks.webclient.dto.TrackInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.michaelthelin.spotify.model_objects.specification.AlbumSimplified;
import se.michaelthelin.spotify.model_objects.specification.ArtistSimplified;
import se.michaelthelin.spotify.model_objects.specification.Image;
import se.michaelthelin.spotify.model_objects.specification.Track;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrackServiceTest {

    @Mock
    private SpotifyWebClient spotifyWebClient;

    @Mock
    private SearchKeywordTracker tracker;

    @InjectMocks
    private TrackService trackService;

    @Test
    void search는_tracker_save후_Spotify결과를_TrackData와_totalCount로_매핑한다() {
        Track first = track("track-1", "artist-1", "title-1", "album-1", "image-1");
        Track second = track("track-2", "artist-2", "title-2", "album-2", "image-2");
        when(spotifyWebClient.getTrackInfoByKeyword("rock", 0)).thenReturn(TrackInfo.of(new Track[]{first, second}, 25));

        SearchTrackList result = trackService.search("rock", 0);

        InOrder inOrder = inOrder(tracker, spotifyWebClient);
        inOrder.verify(tracker).save("rock");
        inOrder.verify(spotifyWebClient).getTrackInfoByKeyword("rock", 0);
        assertThat(result.getTotalCount()).isEqualTo(25);
        assertThat(result.getTrackDataList()).extracting(TrackData::getTrackId).containsExactly("track-1", "track-2");
        assertThat(result.getTrackDataList().get(0).getArtistName()).isEqualTo("artist-1");
        assertThat(result.getTrackDataList().get(0).getImageUrl()).isEqualTo("image-1");
    }

    @Test
    void search와_detail은_album_image가_없으면_NO_IMAGE를_쓴다() {
        Track noImageTrack = track("track-1", "artist-1", "title-1", "album-1");
        when(spotifyWebClient.getTrackInfoByKeyword("rock", 0)).thenReturn(TrackInfo.of(new Track[]{noImageTrack}, 1));
        when(spotifyWebClient.getDetailTrackInfo("track-1")).thenReturn(noImageTrack);

        SearchTrackList searchResult = trackService.search("rock", 0);
        TrackData detailResult = trackService.getDetail("track-1");

        assertThat(searchResult.getTrackDataList().get(0).getImageUrl()).isEqualTo("NO_IMAGE");
        assertThat(detailResult.getImageUrl()).isEqualTo("NO_IMAGE");
    }

    @Test
    void getDetail은_Spotify단건결과를_TrackData로_매핑한다() {
        Track track = track("track-1", "artist-1", "title-1", "album-1", "image-1");
        when(spotifyWebClient.getDetailTrackInfo("track-1")).thenReturn(track);

        TrackData result = trackService.getDetail("track-1");

        assertThat(result.getTrackId()).isEqualTo("track-1");
        assertThat(result.getArtistName()).isEqualTo("artist-1");
        assertThat(result.getTitle()).isEqualTo("title-1");
        assertThat(result.getAlbumName()).isEqualTo("album-1");
        assertThat(result.getImageUrl()).isEqualTo("image-1");
    }

    @Test
    void getKeywordRanking은_tracker결과를_그대로_반환한다() {
        when(tracker.getTopSearchKeywordList()).thenReturn(List.of("rock", "pop"));

        RankingList result = trackService.getKeywordRanking();

        assertThat(result.getKeywordList()).containsExactly("rock", "pop");
    }

    private Track track(String trackId, String artistName, String title, String albumName, String imageUrl) {
        Track track = mock(Track.class);
        ArtistSimplified artist = mock(ArtistSimplified.class);
        AlbumSimplified album = mock(AlbumSimplified.class);
        Image image = mock(Image.class);

        when(track.getId()).thenReturn(trackId);
        when(track.getName()).thenReturn(title);
        when(track.getArtists()).thenReturn(new ArtistSimplified[]{artist});
        when(artist.getName()).thenReturn(artistName);
        when(track.getAlbum()).thenReturn(album);
        when(album.getName()).thenReturn(albumName);
        when(album.getImages()).thenReturn(new Image[]{image});
        when(image.getUrl()).thenReturn(imageUrl);
        return track;
    }

    private Track track(String trackId, String artistName, String title, String albumName) {
        Track track = mock(Track.class);
        ArtistSimplified artist = mock(ArtistSimplified.class);
        AlbumSimplified album = mock(AlbumSimplified.class);

        when(track.getId()).thenReturn(trackId);
        when(track.getName()).thenReturn(title);
        when(track.getArtists()).thenReturn(new ArtistSimplified[]{artist});
        when(artist.getName()).thenReturn(artistName);
        when(track.getAlbum()).thenReturn(album);
        when(album.getName()).thenReturn(albumName);
        when(album.getImages()).thenReturn(new Image[0]);
        return track;
    }
}
