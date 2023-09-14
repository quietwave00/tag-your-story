package com.tagstory.api.domain.tracks.dto.response;

import com.tagstory.core.domain.tracks.service.dto.response.SearchTracks;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SearchTracksResponse {
    private String trackId;
    private String title;
    private String artistName;
    private String albumName;
    private String imageUrl;

    public static SearchTracksResponse from(SearchTracks searchTracks) {
        return SearchTracksResponse.builder()
                .trackId(searchTracks.getTrackId())
                .title(searchTracks.getTitle())
                .artistName(searchTracks.getArtistName())
                .albumName(searchTracks.getAlbumName())
                .imageUrl(searchTracks.getImageUrl())
                .build();
    }
}
