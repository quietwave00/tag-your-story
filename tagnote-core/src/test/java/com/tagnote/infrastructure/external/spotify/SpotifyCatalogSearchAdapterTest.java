package com.tagnote.infrastructure.external.spotify;

import com.tagnote.application.catalog.search.model.CatalogSearchItem;
import com.tagnote.application.catalog.search.model.CatalogSearchResult;
import com.tagnote.application.catalog.search.model.CatalogSearchSubjectType;
import com.tagnote.core.domain.tracks.webclient.SpotifyWebClient;
import com.tagnote.core.domain.tracks.webclient.dto.SpotifyCatalogSearchInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.michaelthelin.spotify.model_objects.specification.AlbumSimplified;
import se.michaelthelin.spotify.model_objects.specification.Artist;
import se.michaelthelin.spotify.model_objects.specification.ArtistSimplified;
import se.michaelthelin.spotify.model_objects.specification.Image;
import se.michaelthelin.spotify.model_objects.specification.Track;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SpotifyCatalogSearchAdapterTest {

    @Mock
    private SpotifyWebClient spotifyWebClient;

    @InjectMocks
    private SpotifyCatalogSearchAdapter adapter;

    @Test
    void search는_Track_Album_Artist를_공통모델로_매핑하고_total을_합산한다() {
        Track track = track("track-1", "Song", "Track Artist", "Track Album", "track-image");
        AlbumSimplified album = album("album-1", "Album", "Album Artist", "album-image");
        Artist artist = artist("artist-1", "Artist", "artist-image");
        when(spotifyWebClient.searchCatalog("queen", 1)).thenReturn(SpotifyCatalogSearchInfo.of(
                new Track[]{track},
                new AlbumSimplified[]{album},
                new Artist[]{artist},
                100,
                20,
                3
        ));

        CatalogSearchResult result = adapter.search("queen", 1);

        verify(spotifyWebClient).searchCatalog("queen", 1);
        assertThat(result.getTotalCount()).isEqualTo(123L);
        assertThat(result.getItems()).extracting(CatalogSearchItem::getSubjectType)
                .containsExactly(
                        CatalogSearchSubjectType.TRACK,
                        CatalogSearchSubjectType.ALBUM,
                        CatalogSearchSubjectType.ARTIST
                );
        assertThat(result.getItems().get(0).getAlbumName()).isEqualTo("Track Album");
        assertThat(result.getItems().get(1).getTitle()).isEqualTo("Album");
        assertThat(result.getItems().get(1).getAlbumName()).isNull();
        assertThat(result.getItems().get(2).getTitle()).isEqualTo("Artist");
        assertThat(result.getItems().get(2).getArtistName()).isNull();
    }

    @Test
    void search는_이미지와_artist가_없으면_NO_IMAGE와_null을_사용한다() {
        AlbumSimplified album = mock(AlbumSimplified.class);
        when(album.getId()).thenReturn("album-1");
        when(album.getName()).thenReturn("Album");
        when(album.getArtists()).thenReturn(new ArtistSimplified[0]);
        when(album.getImages()).thenReturn(new Image[0]);
        when(spotifyWebClient.searchCatalog("album", 0)).thenReturn(SpotifyCatalogSearchInfo.of(
                new Track[0], new AlbumSimplified[]{album}, new Artist[0], 0, 1, 0
        ));

        CatalogSearchResult result = adapter.search("album", 0);

        assertThat(result.getItems().get(0).getArtistName()).isNull();
        assertThat(result.getItems().get(0).getImageUrl()).isEqualTo("NO_IMAGE");
    }

    private Track track(String id, String title, String artistName, String albumName, String imageUrl) {
        Track track = mock(Track.class);
        AlbumSimplified album = mock(AlbumSimplified.class);
        ArtistSimplified artist = mock(ArtistSimplified.class);
        Image image = mock(Image.class);
        when(track.getId()).thenReturn(id);
        when(track.getName()).thenReturn(title);
        when(track.getArtists()).thenReturn(new ArtistSimplified[]{artist});
        when(track.getAlbum()).thenReturn(album);
        when(artist.getName()).thenReturn(artistName);
        when(album.getName()).thenReturn(albumName);
        when(album.getImages()).thenReturn(new Image[]{image});
        when(image.getUrl()).thenReturn(imageUrl);
        return track;
    }

    private AlbumSimplified album(String id, String title, String artistName, String imageUrl) {
        AlbumSimplified album = mock(AlbumSimplified.class);
        ArtistSimplified artist = mock(ArtistSimplified.class);
        Image image = mock(Image.class);
        when(album.getId()).thenReturn(id);
        when(album.getName()).thenReturn(title);
        when(album.getArtists()).thenReturn(new ArtistSimplified[]{artist});
        when(album.getImages()).thenReturn(new Image[]{image});
        when(artist.getName()).thenReturn(artistName);
        when(image.getUrl()).thenReturn(imageUrl);
        return album;
    }

    private Artist artist(String id, String name, String imageUrl) {
        Artist artist = mock(Artist.class);
        Image image = mock(Image.class);
        when(artist.getId()).thenReturn(id);
        when(artist.getName()).thenReturn(name);
        when(artist.getImages()).thenReturn(new Image[]{image});
        when(image.getUrl()).thenReturn(imageUrl);
        return artist;
    }
}
