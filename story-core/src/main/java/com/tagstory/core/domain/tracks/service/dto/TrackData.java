package com.tagstory.core.domain.tracks.service.dto;

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

    public static TrackData of(String trackId, String artistName, String title, String albumName, String imageUrl) {
        return TrackData.builder()
                .trackId(trackId)
                .artistName(artistName)
                .title(title)
                .albumName(albumName)
                .imageUrl(imageUrl)
                .build();
    }
}
