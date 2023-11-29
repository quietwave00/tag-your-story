package com.tagstory.api.domain.tracks.dto.response;

import com.tagstory.core.domain.tracks.service.dto.TrackData;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DetailTrackResponse {
    private String trackId;
    private String title;
    private String artistName;
    private String albumName;
    private String imageUrl;

    public static DetailTrackResponse from(TrackData detailTrack) {
        return builder()
                .trackId(detailTrack.getTrackId())
                .title(detailTrack.getTitle())
                .artistName(detailTrack.getArtistName())
                .albumName(detailTrack.getAlbumName())
                .imageUrl(detailTrack.getImageUrl())
                .build();
    }
}
