package com.tagstory.api.domain.tracks.dto.response;

import com.tagstory.core.domain.tracks.service.dto.response.SearchTracksResponse;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SearchTracks {
    private String trackId;
    private String title;
    private String artistName;
    private String albumName;
    private String imageUrl;

    public static SearchTracks create(SearchTracksResponse searchTracksResponse) {
        return SearchTracks.builder()
                .trackId(searchTracksResponse.getTrackId())
                .title(searchTracksResponse.getTitle())
                .artistName(searchTracksResponse.getArtistName())
                .albumName(searchTracksResponse.getAlbumName())
                .imageUrl(searchTracksResponse.getImageUrl())
                .build();
    }
}
