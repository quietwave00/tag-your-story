package com.tagnote.infrastructure.external.spotify.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

/** Only the Spotify response fields consumed by this server. */
public final class SpotifyJson {
    private SpotifyJson() {}

    @Getter @Setter @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Image { private String url; }

    @Getter @Setter @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Artist {
        private String id;
        private String name;
        private Image[] images;
        private String[] genres;
    }

    @Getter @Setter @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Album {
        private String id;
        private String name;
        private Artist[] artists;
        private Image[] images;
        @JsonAlias("release_date") private String releaseDate;
        @JsonAlias("album_type") private String albumType;
        @JsonAlias("total_tracks") private Integer totalTracks;
        private Page<Track> tracks;
    }

    @Getter @Setter @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Track {
        private String id;
        private String name;
        private Artist[] artists;
        private Album album;
        @JsonAlias("duration_ms") private Integer durationMs;
        @JsonAlias("disc_number") private Integer discNumber;
        @JsonAlias("track_number") private Integer trackNumber;
        @JsonAlias("external_ids") private Map<String, String> externalIds;
    }

    @Getter @Setter @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Page<T> {
        private T[] items;
        private Integer total;
    }

    @Getter @Setter @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Search {
        private Page<Track> tracks;
        private Page<Album> albums;
        private Page<Artist> artists;
    }

    @Getter @Setter @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Token {
        @JsonAlias("access_token") private String accessToken;
        @JsonAlias("expires_in") private Integer expiresIn;
    }
}
