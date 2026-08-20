package com.tagnote.domain.tracks.service;

import com.tagnote.application.catalog.search.port.SearchKeywordRankingReader;
import com.tagnote.core.domain.tracks.service.TrackService;
import com.tagnote.core.domain.tracks.service.dto.TrackData;
import com.tagnote.core.domain.tracks.service.dto.response.RankingList;
import com.tagnote.core.domain.tracks.webclient.SpotifyWebClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.michaelthelin.spotify.model_objects.specification.AlbumSimplified;
import se.michaelthelin.spotify.model_objects.specification.ArtistSimplified;
import se.michaelthelin.spotify.model_objects.specification.Image;
import se.michaelthelin.spotify.model_objects.specification.Track;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrackServiceTest {

    @Mock
    private SpotifyWebClient spotifyWebClient;

    @Mock
    private SearchKeywordRankingReader searchKeywordRankingReader;

    @InjectMocks
    private TrackService trackService;

    @Test
    void getDetail은_album_image가_없으면_NO_IMAGE를_쓴다() {
        Track noImageTrack = track("track-1", "artist-1", "title-1", "album-1");
        when(spotifyWebClient.getDetailTrackInfo("track-1")).thenReturn(noImageTrack);

        TrackData detailResult = trackService.getDetail("track-1");

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
        when(searchKeywordRankingReader.getTopSearchKeywordList()).thenReturn(List.of("rock", "pop"));

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
