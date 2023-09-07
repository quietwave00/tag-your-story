package com.tagstory.core.domain.tracks.service.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DetailTrack {
    private String trackId;
    private String title;
    private String artistName;
    private String albumName;
    private String imageUrl;

    public static DetailTrack onComplete(String trackId, String artistName, String title, String albumName, String imageUrl) {
        return DetailTrack.builder()
                .trackId(trackId)
                .title(title)
                .artistName(artistName)
                .albumName(albumName)
                .imageUrl(imageUrl)
                .build();
    }
}
