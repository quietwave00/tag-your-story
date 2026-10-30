package com.tagnote.infrastructure.external.spotify;

import com.tagnote.application.catalog.detail.model.CatalogReleaseType;
import com.tagnote.application.catalog.detail.model.SpotifyAlbumDetail;
import com.tagnote.application.catalog.detail.model.SpotifyAlbumSummary;
import com.tagnote.application.catalog.detail.model.SpotifyAlbumTrack;
import com.tagnote.application.catalog.detail.model.SpotifyArtistDetail;
import com.tagnote.application.catalog.detail.model.SpotifyCatalogPage;
import com.tagnote.application.catalog.detail.port.SpotifyCatalogDetailProvider;
import com.tagnote.core.domain.tracks.webclient.SpotifyWebClient;
import com.tagnote.core.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import se.michaelthelin.spotify.model_objects.specification.Album;
import se.michaelthelin.spotify.model_objects.specification.AlbumSimplified;
import se.michaelthelin.spotify.model_objects.specification.Artist;
import se.michaelthelin.spotify.model_objects.specification.ArtistSimplified;
import se.michaelthelin.spotify.model_objects.specification.Image;
import se.michaelthelin.spotify.model_objects.specification.Paging;
import se.michaelthelin.spotify.model_objects.specification.TrackSimplified;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class SpotifyCatalogDetailAdapter implements SpotifyCatalogDetailProvider {
    private static final String NO_IMAGE = "NO_IMAGE";
    private static final int TRACK_PAGE_SIZE = 10;

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
        Integer totalTracks = tracks == null ? null : tracks.getTotal();
        String albumType = album.getAlbumType() == null ? null : album.getAlbumType().toString();
        Long observedDurationMs = "album".equalsIgnoreCase(albumType) && totalTracks != null && totalTracks > 6
                ? observedDurationMs(spotifyAlbumId, tracks, totalTracks) : null;
        return new SpotifyAlbumDetail(album.getId(), album.getName(), imageUrl(album.getImages()),
                album.getReleaseDate(), totalTracks, artists, albumType, observedDurationMs);
    }

    @Override
    public SpotifyArtistDetail getArtist(String spotifyArtistId) {
        Artist artist = client.getDetailArtistInfo(spotifyArtistId);
        String[] genres = artist.getGenres();
        return new SpotifyArtistDetail(artist.getId(), artist.getName(), imageUrl(artist.getImages()),
                genres == null ? List.of() : Arrays.asList(genres));
    }

    @Override
    public SpotifyCatalogPage<SpotifyAlbumSummary> getArtistAlbums(String spotifyArtistId, int page) {
        Paging<AlbumSimplified> result = client.getArtistAlbums(spotifyArtistId, page);
        AlbumSimplified[] albums = result.getItems();
        List<SpotifyAlbumSummary> items = albums == null ? List.of() : Arrays.stream(albums)
                .map(album -> new SpotifyAlbumSummary(album.getId(), album.getName(),
                        imageUrl(album.getImages()), album.getReleaseDate(), artistAlbumReleaseType(album)))
                .toList();
        return new SpotifyCatalogPage<>(items, result.getTotal());
    }

    private CatalogReleaseType artistAlbumReleaseType(AlbumSimplified album) {
        String albumType = album.getAlbumType() == null ? null : album.getAlbumType().toString();
        if (!"album".equalsIgnoreCase(albumType)) {
            return CatalogReleaseType.classify(albumType, null, null);
        }
        try {
            Paging<TrackSimplified> tracks = client.getAlbumTracks(album.getId(), 0);
            if (tracks == null) {
                return null;
            }
            Integer totalTracks = tracks.getTotal();
            Long durationMs = totalTracks != null && totalTracks > 6
                    ? observedDurationMs(album.getId(), tracks, totalTracks) : null;
            return CatalogReleaseType.classify(albumType, totalTracks, durationMs);
        } catch (CustomException e) {
            return null;
        }
    }

    @Override
    public SpotifyCatalogPage<SpotifyAlbumTrack> getAlbumTracks(String spotifyAlbumId, int page) {
        Paging<TrackSimplified> result = client.getAlbumTracks(spotifyAlbumId, page);
        TrackSimplified[] tracks = result.getItems();
        List<SpotifyAlbumTrack> items = tracks == null ? List.of() : Arrays.stream(tracks)
                .map(track -> new SpotifyAlbumTrack(track.getId(), track.getName(),
                        firstArtistName(track.getArtists()), track.getDiscNumber(),
                        track.getTrackNumber(), track.getDurationMs()))
                .toList();
        return new SpotifyCatalogPage<>(items, result.getTotal());
    }

    private String firstArtistName(ArtistSimplified[] artists) {
        return artists == null || artists.length == 0 || artists[0] == null ? null : artists[0].getName();
    }

    private Long observedDurationMs(String albumId, Paging<TrackSimplified> initialPage, int totalTracks) {
        TrackSimplified[] initial = initialPage.getItems();
        int processed = 0;
        long durationMs = 0;
        if (initial != null && initial.length <= totalTracks
                && (initial.length == totalTracks || initial.length % TRACK_PAGE_SIZE == 0)) {
            Long initialDuration = sumDuration(initial);
            if (initialDuration == null) {
                return null;
            }
            durationMs = initialDuration;
            processed = initial.length;
            if (durationMs >= CatalogReleaseType.THIRTY_MINUTES_MS) {
                return durationMs;
            }
        }
        for (int page = processed / TRACK_PAGE_SIZE; processed < totalTracks; page++) {
            TrackSimplified[] items;
            try {
                Paging<TrackSimplified> extraPage = client.getAlbumTracks(albumId, page);
                if (extraPage == null) {
                    return null;
                }
                items = extraPage.getItems();
            } catch (CustomException e) {
                return null;
            }
            if (items == null || items.length == 0 || processed + items.length > totalTracks) {
                return null;
            }
            Long pageDuration = sumDuration(items);
            if (pageDuration == null) {
                return null;
            }
            durationMs += pageDuration;
            processed += items.length;
            if (durationMs >= CatalogReleaseType.THIRTY_MINUTES_MS) {
                return durationMs;
            }
        }
        return durationMs;
    }

    private Long sumDuration(TrackSimplified[] tracks) {
        long durationMs = 0;
        for (TrackSimplified track : tracks) {
            if (track == null) {
                return null;
            }
            Integer trackDurationMs = track.getDurationMs();
            if (trackDurationMs == null || trackDurationMs <= 0) {
                return null;
            }
            durationMs += trackDurationMs;
            if (durationMs >= CatalogReleaseType.THIRTY_MINUTES_MS) {
                return durationMs;
            }
        }
        return durationMs;
    }

    private String imageUrl(Image[] images) {
        return images == null || images.length == 0 || images[0] == null || images[0].getUrl() == null
                ? NO_IMAGE : images[0].getUrl();
    }
}
