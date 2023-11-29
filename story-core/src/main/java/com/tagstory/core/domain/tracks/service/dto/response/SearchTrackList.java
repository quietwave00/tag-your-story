package com.tagstory.core.domain.tracks.service.dto.response;

import com.tagstory.core.domain.tracks.service.dto.TrackData;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class SearchTrackList {
    private List<TrackData> trackDataList;
    private Integer totalCount;

    public static SearchTrackList onComplete(List<TrackData> trackDataList, Integer totalCount) {
        return builder()
                .trackDataList(trackDataList)
                .totalCount(totalCount)
                .build();
    }
}
