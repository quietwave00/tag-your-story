package com.tagstory.api.domain.tracks.dto.response;

import com.tagstory.core.domain.tracks.service.dto.response.DetailTrackResponse;
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

    public static DetailTrack create(DetailTrackResponse detailTrackResponse) {
        return DetailTrack.builder()
                .trackId(detailTrackResponse.getTrackId())
                .title(detailTrackResponse.getTitle())
                .artistName(detailTrackResponse.getArtistName())
                .albumName(detailTrackResponse.getAlbumName())
                .imageUrl(detailTrackResponse.getImageUrl())
                .build();
    }
}
