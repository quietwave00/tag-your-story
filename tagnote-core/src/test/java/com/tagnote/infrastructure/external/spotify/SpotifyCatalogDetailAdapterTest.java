package com.tagnote.infrastructure.external.spotify;

import com.tagnote.application.catalog.detail.model.SpotifyAlbumDetail;
import com.tagnote.application.catalog.detail.model.SpotifyAlbumSummary;
import com.tagnote.application.catalog.detail.model.SpotifyAlbumTrack;
import com.tagnote.application.catalog.detail.model.SpotifyArtistDetail;
import com.tagnote.application.catalog.detail.model.SpotifyCatalogPage;
import com.tagnote.core.domain.tracks.webclient.SpotifyWebClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.michaelthelin.spotify.model_objects.specification.Album;
import se.michaelthelin.spotify.model_objects.specification.AlbumSimplified;
import se.michaelthelin.spotify.model_objects.specification.Artist;
import se.michaelthelin.spotify.model_objects.specification.ArtistSimplified;
import se.michaelthelin.spotify.model_objects.specification.Image;
import se.michaelthelin.spotify.model_objects.specification.Paging;
import se.michaelthelin.spotify.model_objects.specification.TrackSimplified;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SpotifyCatalogDetailAdapterTest {
    @Mock
    private SpotifyWebClient client;

    @InjectMocks
    private SpotifyCatalogDetailAdapter adapter;

    @Test
    void 앨범_상세는_참여_아티스트_순서와_발매일을_유지한다() {
        Album album = mock(Album.class);
        ArtistSimplified first = artistCredit("first", "First");
        ArtistSimplified second = artistCredit("second", "Second");
        Image image = mock(Image.class);
        @SuppressWarnings("unchecked")
        Paging<TrackSimplified> tracks = mock(Paging.class);
        when(client.getDetailAlbumInfo("album-1")).thenReturn(album);
        when(album.getId()).thenReturn("album-1");
        when(album.getName()).thenReturn("Album");
        when(album.getReleaseDate()).thenReturn("2024-03-15");
        when(album.getTracks()).thenReturn(tracks);
        when(tracks.getTotal()).thenReturn(12);
        when(album.getArtists()).thenReturn(new ArtistSimplified[]{first, second});
        when(album.getImages()).thenReturn(new Image[]{image});
        when(image.getUrl()).thenReturn("cover-url");

        SpotifyAlbumDetail detail = adapter.getAlbum("album-1");

        assertThat(detail.spotifyId()).isEqualTo("album-1");
        assertThat(detail.title()).isEqualTo("Album");
        assertThat(detail.imageUrl()).isEqualTo("cover-url");
        assertThat(detail.releaseDate()).isEqualTo("2024-03-15");
        assertThat(detail.totalTracks()).isEqualTo(12);
        assertThat(detail.artists()).extracting(SpotifyAlbumDetail.ArtistCredit::spotifyId)
                .containsExactly("first", "second");
        verify(client).getDetailAlbumInfo("album-1");
    }

    @Test
    void 아티스트_상세는_장르를_반환하고_이미지가_없으면_기본값을_사용한다() {
        Artist artist = mock(Artist.class);
        when(client.getDetailArtistInfo("artist-1")).thenReturn(artist);
        when(artist.getId()).thenReturn("artist-1");
        when(artist.getName()).thenReturn("Queen");
        when(artist.getGenres()).thenReturn(new String[]{"classic rock", "glam rock"});
        when(artist.getImages()).thenReturn(new Image[0]);

        SpotifyArtistDetail detail = adapter.getArtist("artist-1");

        assertThat(detail.spotifyId()).isEqualTo("artist-1");
        assertThat(detail.name()).isEqualTo("Queen");
        assertThat(detail.genres()).containsExactly("classic rock", "glam rock");
        assertThat(detail.imageUrl()).isEqualTo("NO_IMAGE");
        verify(client).getDetailArtistInfo("artist-1");
    }

    @Test
    void 앨범_상세는_이미지와_참여_아티스트가_없어도_빈_값을_반환한다() {
        Album album = mock(Album.class);
        when(client.getDetailAlbumInfo("album-2")).thenReturn(album);
        when(album.getImages()).thenReturn(null);
        when(album.getArtists()).thenReturn(null);

        SpotifyAlbumDetail detail = adapter.getAlbum("album-2");

        assertThat(detail.imageUrl()).isEqualTo("NO_IMAGE");
        assertThat(detail.artists()).isEmpty();
    }

    @Test
    void 아티스트_장르가_없으면_빈_목록을_반환한다() {
        Artist artist = mock(Artist.class);
        when(client.getDetailArtistInfo("artist-2")).thenReturn(artist);
        when(artist.getGenres()).thenReturn(null);

        SpotifyArtistDetail detail = adapter.getArtist("artist-2");

        assertThat(detail.genres()).isEmpty();
        assertThat(detail.imageUrl()).isEqualTo("NO_IMAGE");
    }

    @Test
    void 아티스트_앨범_목록은_Spotify_순서와_전체_건수를_유지한다() {
        @SuppressWarnings("unchecked")
        Paging<AlbumSimplified> page = mock(Paging.class);
        AlbumSimplified first = mock(AlbumSimplified.class);
        AlbumSimplified second = mock(AlbumSimplified.class);
        Image image = mock(Image.class);
        when(client.getArtistAlbums("artist-1", 2)).thenReturn(page);
        when(page.getItems()).thenReturn(new AlbumSimplified[]{first, second});
        when(page.getTotal()).thenReturn(27);
        when(first.getId()).thenReturn("album-1");
        when(first.getName()).thenReturn("First");
        when(first.getReleaseDate()).thenReturn("2020");
        when(first.getImages()).thenReturn(new Image[]{image});
        when(image.getUrl()).thenReturn("cover");
        when(second.getId()).thenReturn("album-2");
        when(second.getName()).thenReturn("Second");
        when(second.getImages()).thenReturn(new Image[0]);

        SpotifyCatalogPage<SpotifyAlbumSummary> result = adapter.getArtistAlbums("artist-1", 2);

        assertThat(result.items()).extracting(SpotifyAlbumSummary::spotifyId)
                .containsExactly("album-1", "album-2");
        assertThat(result.items().get(0).releaseDate()).isEqualTo("2020");
        assertThat(result.items().get(0).imageUrl()).isEqualTo("cover");
        assertThat(result.items().get(1).imageUrl()).isEqualTo("NO_IMAGE");
        assertThat(result.totalCount()).isEqualTo(27);
        verify(client).getArtistAlbums("artist-1", 2);
    }

    @Test
    void 앨범_트랙_목록은_원본_순서와_디스크_번호를_유지한다() {
        @SuppressWarnings("unchecked")
        Paging<TrackSimplified> page = mock(Paging.class);
        TrackSimplified first = mock(TrackSimplified.class);
        TrackSimplified second = mock(TrackSimplified.class);
        ArtistSimplified artist = mock(ArtistSimplified.class);
        when(artist.getName()).thenReturn("Queen");
        when(client.getAlbumTracks("album-1", 1)).thenReturn(page);
        when(page.getItems()).thenReturn(new TrackSimplified[]{first, second});
        when(page.getTotal()).thenReturn(12);
        when(first.getId()).thenReturn("track-1");
        when(first.getName()).thenReturn("First");
        when(first.getArtists()).thenReturn(new ArtistSimplified[]{artist});
        when(first.getDiscNumber()).thenReturn(2);
        when(first.getTrackNumber()).thenReturn(3);
        when(first.getDurationMs()).thenReturn(180000);
        when(second.getId()).thenReturn("track-2");
        when(second.getName()).thenReturn("Second");
        when(second.getArtists()).thenReturn(new ArtistSimplified[0]);

        SpotifyCatalogPage<SpotifyAlbumTrack> result = adapter.getAlbumTracks("album-1", 1);

        assertThat(result.items()).extracting(SpotifyAlbumTrack::spotifyId)
                .containsExactly("track-1", "track-2");
        assertThat(result.items().get(0).artistName()).isEqualTo("Queen");
        assertThat(result.items().get(0).discNumber()).isEqualTo(2);
        assertThat(result.items().get(0).trackNumber()).isEqualTo(3);
        assertThat(result.items().get(0).durationMs()).isEqualTo(180000);
        assertThat(result.items().get(1).artistName()).isNull();
        assertThat(result.totalCount()).isEqualTo(12);
        verify(client).getAlbumTracks("album-1", 1);
    }

    @Test
    void Spotify_목록이_비어도_전체_건수를_유지한다() {
        @SuppressWarnings("unchecked")
        Paging<TrackSimplified> page = mock(Paging.class);
        when(client.getAlbumTracks("album-2", 3)).thenReturn(page);
        when(page.getItems()).thenReturn(new TrackSimplified[0]);
        when(page.getTotal()).thenReturn(29);

        SpotifyCatalogPage<SpotifyAlbumTrack> result = adapter.getAlbumTracks("album-2", 3);

        assertThat(result.items()).isEmpty();
        assertThat(result.totalCount()).isEqualTo(29);
    }

    private ArtistSimplified artistCredit(String id, String name) {
        ArtistSimplified artist = mock(ArtistSimplified.class);
        when(artist.getId()).thenReturn(id);
        when(artist.getName()).thenReturn(name);
        return artist;
    }
}
