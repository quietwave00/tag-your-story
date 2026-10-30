package com.tagnote.core.domain.tracks.service.dto;

import lombok.Builder;
import lombok.Getter;


@Getter
@Builder
public class TrackData {
    private String trackId;
    private String artistName;
    private String title;
    private String albumName;
    private String imageUrl;
    private String spotifyArtistId;
    private String spotifyAlbumId;

    public static TrackData of(String trackId, String artistName, String title, String albumName, String imageUrl,
                               String spotifyArtistId, String spotifyAlbumId) {
        return TrackData.builder()
                .trackId(trackId)
                .artistName(artistName)
                .title(title)
                .albumName(albumName)
                .imageUrl(imageUrl)
                .spotifyArtistId(spotifyArtistId)
                .spotifyAlbumId(spotifyAlbumId)
                .build();
    }
}
