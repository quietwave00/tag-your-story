package com.tagnote.infrastructure.external.spotify;

import com.tagnote.application.catalog.detail.model.SpotifyAlbumDetail;
import com.tagnote.application.catalog.detail.model.SpotifyArtistDetail;
import com.tagnote.core.domain.tracks.webclient.SpotifyWebClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.michaelthelin.spotify.model_objects.specification.Album;
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

    private ArtistSimplified artistCredit(String id, String name) {
        ArtistSimplified artist = mock(ArtistSimplified.class);
        when(artist.getId()).thenReturn(id);
        when(artist.getName()).thenReturn(name);
        return artist;
    }
}
