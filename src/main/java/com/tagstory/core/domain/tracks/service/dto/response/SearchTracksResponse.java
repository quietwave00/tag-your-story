package com.tagstory.core.domain.tracks.service.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SearchTracksResponse {
    private String trackId;private String title;

    private String artistName;

    private String albumName;
    private String imageUrl;

    public static SearchTracksResponse onComplete(String trackId, String artistName, String title, String albumName, String imageUrl) {
        return SearchTracksResponse.builder()
                .trackId(trackId)
                .title(title)
                .artistName(artistName)
                .albumName(albumName)
                .imageUrl(imageUrl)
                .build();
    }
}
