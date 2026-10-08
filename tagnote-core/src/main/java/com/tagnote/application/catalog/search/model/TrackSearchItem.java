package com.tagnote.application.catalog.search.model;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TrackSearchItem {

    private String spotifyTrackId;
    private String artistName;
    private String title;
    private String albumName;
    private String imageUrl;

    public static TrackSearchItem of(
            String spotifyTrackId,
            String artistName,
            String title,
            String albumName,
            String imageUrl
    ) {
        return TrackSearchItem.builder()
                .spotifyTrackId(spotifyTrackId)
                .artistName(artistName)
                .title(title)
                .albumName(albumName)
                .imageUrl(imageUrl)
                .build();
    }
}
