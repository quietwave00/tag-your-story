package com.tagnote.infrastructure.external.spotify;

import com.tagnote.application.catalog.search.model.TrackSearchItem;
import com.tagnote.application.catalog.search.model.TrackSearchResult;
import com.tagnote.core.domain.tracks.webclient.SpotifyWebClient;
import com.tagnote.core.domain.tracks.webclient.dto.TrackInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.michaelthelin.spotify.model_objects.specification.AlbumSimplified;
import se.michaelthelin.spotify.model_objects.specification.ArtistSimplified;
import se.michaelthelin.spotify.model_objects.specification.Image;
import se.michaelthelin.spotify.model_objects.specification.Track;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SpotifyTrackSearchAdapterTest {

    @Mock
    private SpotifyWebClient spotifyWebClient;

    @InjectMocks
    private SpotifyTrackSearchAdapter adapter;

    @Test
    void search는_Spotify_결과의_순서와_totalCount를_application_model로_매핑한다() {
        Track first = track("track-1", "artist-1", "title-1", "album-1", "image-1");
        Track second = track("track-2", "artist-2", "title-2", "album-2", "image-2");
        when(spotifyWebClient.getTrackInfoByKeyword("rock", 2))
                .thenReturn(TrackInfo.of(new Track[]{first, second}, 25));

        TrackSearchResult result = adapter.search("rock", 2);

        verify(spotifyWebClient).getTrackInfoByKeyword("rock", 2);
        assertThat(result.getItems()).extracting(TrackSearchItem::getSpotifyTrackId)
                .containsExactly("track-1", "track-2");
        assertThat(result.getTotalCount()).isEqualTo(25);
        assertThat(result.getItems().get(0).getArtistName()).isEqualTo("artist-1");
        assertThat(result.getItems().get(0).getTitle()).isEqualTo("title-1");
        assertThat(result.getItems().get(0).getAlbumName()).isEqualTo("album-1");
        assertThat(result.getItems().get(0).getImageUrl()).isEqualTo("image-1");
    }

    @Test
    void search는_album_image가_없으면_NO_IMAGE를_사용한다() {
        Track noImageTrack = track("track-1", "artist-1", "title-1", "album-1");
        when(spotifyWebClient.getTrackInfoByKeyword("rock", 0))
                .thenReturn(TrackInfo.of(new Track[]{noImageTrack}, 1));

        TrackSearchResult result = adapter.search("rock", 0);

        assertThat(result.getItems().get(0).getImageUrl()).isEqualTo("NO_IMAGE");
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
