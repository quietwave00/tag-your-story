package com.tagnote.infrastructure.external.spotify;

import com.tagnote.application.catalog.importer.model.SpotifyTrackMetadata;
import com.tagnote.core.domain.tracks.webclient.SpotifyWebClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.michaelthelin.spotify.model_objects.specification.AlbumSimplified;
import se.michaelthelin.spotify.model_objects.specification.ArtistSimplified;
import se.michaelthelin.spotify.model_objects.specification.ExternalId;
import se.michaelthelin.spotify.model_objects.specification.Track;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SpotifyTrackMetadataAdapterTest {

    @Mock
    private SpotifyWebClient spotifyWebClient;

    @InjectMocks
    private SpotifyTrackMetadataAdapter adapter;

    @Test
    void Track과_Album의_전체_Artist와_순서를_각각_보존한다() {
        Track track = track(
                new ArtistSimplified[]{artist("artist-a", "A"), artist("artist-b", "B")},
                new ArtistSimplified[]{artist("album-artist", "Various Artists")}
        );
        when(spotifyWebClient.getDetailTrackInfo("track-1")).thenReturn(track);

        SpotifyTrackMetadata metadata = adapter.getTrack("track-1");

        assertThat(metadata.getSpotifyTrackId()).isEqualTo("track-1");
        assertThat(metadata.getIsrc()).isEqualTo("ISRC-1");
        assertThat(metadata.getDurationMs()).isEqualTo(240_000);
        assertThat(metadata.getReleaseYear()).isEqualTo(2024);
        assertThat(metadata.getTrackArtists())
                .extracting(artist -> artist.getSpotifyArtistId() + ":" + artist.getPosition())
                .containsExactly("artist-a:0", "artist-b:1");
        assertThat(metadata.getAlbumArtists())
                .extracting(artist -> artist.getSpotifyArtistId() + ":" + artist.getPosition())
                .containsExactly("album-artist:0");
    }

    @Test
    void 중복_Artist_ID는_첫_등장만_보존하고_position을_연속_정규화한다() {
        Track track = track(
                new ArtistSimplified[]{
                        artist("artist-a", "A"),
                        artist("artist-a", "A duplicate"),
                        artist("artist-b", "B")
                },
                new ArtistSimplified[]{artist("album-artist", "Album Artist")}
        );
        when(spotifyWebClient.getDetailTrackInfo("track-1")).thenReturn(track);

        SpotifyTrackMetadata metadata = adapter.getTrack("track-1");

        assertThat(metadata.getTrackArtists())
                .extracting(artist -> artist.getName() + ":" + artist.getPosition())
                .containsExactly("A:0", "B:1");
    }

    @Test
    void Track_Artist가_없으면_불완전한_Catalog를_만들지_않는다() {
        Track track = mock(Track.class);
        AlbumSimplified album = mock(AlbumSimplified.class);
        when(track.getId()).thenReturn("track-1");
        when(track.getName()).thenReturn("title");
        when(track.getDurationMs()).thenReturn(240_000);
        when(track.getArtists()).thenReturn(new ArtistSimplified[0]);
        when(track.getAlbum()).thenReturn(album);
        when(spotifyWebClient.getDetailTrackInfo("track-1")).thenReturn(track);

        assertThatThrownBy(() -> adapter.getTrack("track-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("track artists");
    }

    private Track track(ArtistSimplified[] trackArtists, ArtistSimplified[] albumArtists) {
        Track track = mock(Track.class);
        AlbumSimplified album = mock(AlbumSimplified.class);
        ExternalId externalId = mock(ExternalId.class);

        when(track.getId()).thenReturn("track-1");
        when(track.getName()).thenReturn("title");
        when(track.getDurationMs()).thenReturn(240_000);
        when(track.getArtists()).thenReturn(trackArtists);
        when(track.getAlbum()).thenReturn(album);
        when(track.getExternalIds()).thenReturn(externalId);
        when(externalId.getExternalIds()).thenReturn(Map.of("isrc", "ISRC-1"));
        when(album.getId()).thenReturn("album-1");
        when(album.getName()).thenReturn("album");
        when(album.getReleaseDate()).thenReturn("2024-03-15");
        when(album.getArtists()).thenReturn(albumArtists);
        return track;
    }

    private ArtistSimplified artist(String id, String name) {
        ArtistSimplified artist = mock(ArtistSimplified.class);
        when(artist.getId()).thenReturn(id);
        when(artist.getName()).thenReturn(name);
        return artist;
    }
}
