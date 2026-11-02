package com.tagnote.infrastructure.external.spotify;

import com.tagnote.application.catalog.detail.model.CatalogReleaseType;
import com.tagnote.application.catalog.detail.model.SpotifyAlbumDetail;
import com.tagnote.application.catalog.detail.model.SpotifyAlbumSummary;
import com.tagnote.application.catalog.detail.model.SpotifyAlbumTrack;
import com.tagnote.application.catalog.detail.model.SpotifyArtistDetail;
import com.tagnote.application.catalog.detail.model.SpotifyCatalogPage;
import com.tagnote.application.catalog.detail.port.SpotifyCatalogDetailProvider;
import com.tagnote.core.exception.CustomException;
import com.tagnote.infrastructure.external.spotify.dto.SpotifyJson.Album;
import com.tagnote.infrastructure.external.spotify.dto.SpotifyJson.Artist;
import com.tagnote.infrastructure.external.spotify.dto.SpotifyJson.Image;
import com.tagnote.infrastructure.external.spotify.dto.SpotifyJson.Page;
import com.tagnote.infrastructure.external.spotify.dto.SpotifyJson.Track;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SpotifyCatalogDetailAdapter implements SpotifyCatalogDetailProvider {
    private static final String NO_IMAGE = "NO_IMAGE";
    private static final int TRACK_PAGE_SIZE = 10;

    private final SpotifyHttpClient client;

    @Override
    public SpotifyAlbumDetail getAlbum(String spotifyAlbumId) {
        Album album = client.album(spotifyAlbumId);
        Artist[] credits = album.getArtists();
        List<SpotifyAlbumDetail.ArtistCredit> artists = credits == null ? List.of() : Arrays.stream(credits)
                .filter(Objects::nonNull)
                .map(artist -> new SpotifyAlbumDetail.ArtistCredit(artist.getId(), artist.getName()))
                .toList();
        Page<Track> tracks = album.getTracks();
        Integer totalTracks = tracks == null ? null : tracks.getTotal();
        String albumType = album.getAlbumType();
        Long observedDurationMs = "album".equalsIgnoreCase(albumType) && totalTracks != null && totalTracks > 6
                ? observedDurationMs(spotifyAlbumId, tracks, totalTracks) : null;
        return new SpotifyAlbumDetail(album.getId(), album.getName(), imageUrl(album.getImages()),
                album.getReleaseDate(), totalTracks, artists, albumType, observedDurationMs);
    }

    @Override
    public SpotifyArtistDetail getArtist(String spotifyArtistId) {
        Artist artist = client.artist(spotifyArtistId);
        String[] genres = artist.getGenres();
        return new SpotifyArtistDetail(artist.getId(), artist.getName(), imageUrl(artist.getImages()),
                genres == null ? List.of() : Arrays.asList(genres));
    }

    @Override
    public SpotifyCatalogPage<SpotifyAlbumSummary> getArtistAlbums(String spotifyArtistId, int page) {
        Page<Album> result = client.artistAlbums(spotifyArtistId, page);
        Album[] albums = result.getItems();
        List<SpotifyAlbumSummary> items = albums == null ? List.of() : Arrays.stream(albums)
                .map(album -> new SpotifyAlbumSummary(album.getId(), album.getName(),
                        imageUrl(album.getImages()), album.getReleaseDate(), artistAlbumReleaseType(album)))
                .toList();
        return new SpotifyCatalogPage<>(items, result.getTotal());
    }

    private CatalogReleaseType artistAlbumReleaseType(Album album) {
        String albumType = album.getAlbumType();
        if (!"album".equalsIgnoreCase(albumType)) {
            return CatalogReleaseType.classify(albumType, null, null);
        }
        Integer knownTotal = album.getTotalTracks();
        if (knownTotal != null && knownTotal <= 6) {
            return CatalogReleaseType.classify(albumType, knownTotal, null);
        }
        try {
            Page<Track> tracks = client.albumTracks(album.getId(), 0);
            if (tracks == null) {
                return null;
            }
            if (knownTotal != null && !Objects.equals(tracks.getTotal(), knownTotal)) {
                return null;
            }
            Integer totalTracks = knownTotal != null ? knownTotal : tracks.getTotal();
            Track[] firstPage = tracks.getItems();
            if (totalTracks != null && totalTracks > 6
                    && (firstPage == null || firstPage.length != Math.min(totalTracks, TRACK_PAGE_SIZE))) {
                return null;
            }
            Long durationMs = totalTracks != null && totalTracks > 6
                    ? observedDurationMs(album.getId(), tracks, totalTracks) : null;
            return CatalogReleaseType.classify(albumType, totalTracks, durationMs);
        } catch (CustomException e) {
            return null;
        }
    }

    @Override
    public SpotifyCatalogPage<SpotifyAlbumTrack> getAlbumTracks(String spotifyAlbumId, int page) {
        Page<Track> result = client.albumTracks(spotifyAlbumId, page);
        Track[] tracks = result.getItems();
        List<SpotifyAlbumTrack> items = tracks == null ? List.of() : Arrays.stream(tracks)
                .map(track -> new SpotifyAlbumTrack(track.getId(), track.getName(),
                        firstArtistName(track.getArtists()), track.getDiscNumber(),
                        track.getTrackNumber(), track.getDurationMs()))
                .toList();
        return new SpotifyCatalogPage<>(items, result.getTotal());
    }

    private String firstArtistName(Artist[] artists) {
        return artists == null || artists.length == 0 || artists[0] == null ? null : artists[0].getName();
    }

    private Long observedDurationMs(String albumId, Page<Track> initialPage, int totalTracks) {
        Track[] initial = initialPage.getItems();
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
            Track[] items;
            try {
                Page<Track> extraPage = client.albumTracks(albumId, page);
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

    private Long sumDuration(Track[] tracks) {
        long durationMs = 0;
        for (Track track : tracks) {
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
