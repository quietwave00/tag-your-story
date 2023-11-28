package com.tagstory.core.domain.tracks.webclient.dto;

import lombok.Builder;
import lombok.Getter;
import se.michaelthelin.spotify.model_objects.specification.Track;

@Builder
@Getter
public class TrackInfo {
    private Track[] tracks;
    private Integer totalCount;

    public static TrackInfo of(Track[] tracks, Integer totalCount) {
        return TrackInfo.builder()
                .tracks(tracks)
                .totalCount(totalCount)
                .build();
    }
}
