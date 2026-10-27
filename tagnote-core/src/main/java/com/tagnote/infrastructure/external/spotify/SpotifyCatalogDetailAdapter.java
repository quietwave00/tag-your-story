package com.tagnote.infrastructure.external.spotify;

import com.tagnote.application.catalog.detail.model.SpotifyAlbumDetail;
import com.tagnote.application.catalog.detail.model.SpotifyArtistDetail;
import com.tagnote.application.catalog.detail.port.SpotifyCatalogDetailProvider;
import com.tagnote.core.domain.tracks.webclient.SpotifyWebClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import se.michaelthelin.spotify.model_objects.specification.Album;
import se.michaelthelin.spotify.model_objects.specification.Artist;
import se.michaelthelin.spotify.model_objects.specification.ArtistSimplified;
import se.michaelthelin.spotify.model_objects.specification.Image;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class SpotifyCatalogDetailAdapter implements SpotifyCatalogDetailProvider {
    private static final String NO_IMAGE = "NO_IMAGE";

    private final SpotifyWebClient client;

    @Override
    public SpotifyAlbumDetail getAlbum(String spotifyAlbumId) {
        Album album = client.getDetailAlbumInfo(spotifyAlbumId);
        ArtistSimplified[] credits = album.getArtists();
        List<SpotifyAlbumDetail.ArtistCredit> artists = credits == null ? List.of() : Arrays.stream(credits)
                .filter(Objects::nonNull)
                .map(artist -> new SpotifyAlbumDetail.ArtistCredit(artist.getId(), artist.getName()))
                .toList();
        var tracks = album.getTracks();
        return new SpotifyAlbumDetail(album.getId(), album.getName(), imageUrl(album.getImages()),
                album.getReleaseDate(), tracks == null ? null : tracks.getTotal(), artists);
    }

    @Override
    public SpotifyArtistDetail getArtist(String spotifyArtistId) {
        Artist artist = client.getDetailArtistInfo(spotifyArtistId);
        String[] genres = artist.getGenres();
        return new SpotifyArtistDetail(artist.getId(), artist.getName(), imageUrl(artist.getImages()),
                genres == null ? List.of() : Arrays.asList(genres));
    }

    private String imageUrl(Image[] images) {
        return images == null || images.length == 0 || images[0] == null || images[0].getUrl() == null
                ? NO_IMAGE : images[0].getUrl();
    }
}
