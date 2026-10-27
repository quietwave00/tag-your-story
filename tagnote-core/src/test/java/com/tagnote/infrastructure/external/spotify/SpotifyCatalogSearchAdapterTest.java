package com.tagnote.infrastructure.external.spotify;

import com.tagnote.application.catalog.search.model.CatalogSearchItem;
import com.tagnote.application.catalog.search.model.CatalogSearchResult;
import com.tagnote.application.catalog.search.model.CatalogSearchScope;
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
    void 통합_검색은_곡과_앨범과_가수를_공통_모델로_변환하고_전체_건수를_합산한다() {
        Track track = track("track-1", "Song", "Track Artist", "Track Album", "track-image");
        AlbumSimplified album = album("album-1", "Album", "Album Artist", "album-image");
        Artist artist = artist("artist-1", "Artist", "artist-image");
        when(spotifyWebClient.searchCatalog("queen", 1, "track,album,artist")).thenReturn(SpotifyCatalogSearchInfo.of(
                new Track[]{track},
                new AlbumSimplified[]{album},
                new Artist[]{artist},
                100,
                20,
                3
        ));

        CatalogSearchResult result = adapter.search("queen", 1, CatalogSearchScope.ALL);

        verify(spotifyWebClient).searchCatalog("queen", 1, "track,album,artist");
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
    void 앨범_검색은_이미지와_가수가_없어도_기존_표시_규칙을_유지한다() {
        AlbumSimplified album = mock(AlbumSimplified.class);
        when(album.getId()).thenReturn("album-1");
        when(album.getName()).thenReturn("Album");
        when(album.getArtists()).thenReturn(new ArtistSimplified[0]);
        when(album.getImages()).thenReturn(new Image[0]);
        when(spotifyWebClient.searchCatalog("album", 0, "album")).thenReturn(SpotifyCatalogSearchInfo.of(
                null, new AlbumSimplified[]{album}, null, 0, 1, 0
        ));

        CatalogSearchResult result = adapter.search("album", 0, CatalogSearchScope.ALBUM);

        assertThat(result.getItems().get(0).getArtistName()).isNull();
        assertThat(result.getItems().get(0).getImageUrl()).isEqualTo("NO_IMAGE");
        verify(spotifyWebClient).searchCatalog("album", 0, "album");
    }

    @Test
    void 타입별_검색은_다른_타입의_결과와_건수를_포함하지_않는다() {
        Track track = track("track-1", "Song", "Artist", "Album", "image");
        when(spotifyWebClient.searchCatalog("song", 2, "track")).thenReturn(SpotifyCatalogSearchInfo.of(
                new Track[]{track}, null, null, 47, 200, 300));

        CatalogSearchResult result = adapter.search("song", 2, CatalogSearchScope.TRACK);

        assertThat(result.getItems()).extracting(CatalogSearchItem::getSubjectType)
                .containsExactly(CatalogSearchSubjectType.TRACK);
        assertThat(result.getTotalCount()).isEqualTo(47L);
        verify(spotifyWebClient).searchCatalog("song", 2, "track");
    }

    @Test
    void 가수_검색은_요청하지_않은_결과_그룹이_없어도_동작한다() {
        Artist artist = artist("artist-1", "Queen", "image");
        when(spotifyWebClient.searchCatalog("Queen", 0, "artist")).thenReturn(SpotifyCatalogSearchInfo.of(
                null, null, new Artist[]{artist}, 0, 0, 79));

        CatalogSearchResult result = adapter.search("Queen", 0, CatalogSearchScope.ARTIST);

        assertThat(result.getItems()).extracting(CatalogSearchItem::getSubjectType)
                .containsExactly(CatalogSearchSubjectType.ARTIST);
        assertThat(result.getTotalCount()).isEqualTo(79L);
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
