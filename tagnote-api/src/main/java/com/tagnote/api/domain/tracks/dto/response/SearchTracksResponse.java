package com.tagnote.api.domain.tracks.dto.response;

import com.tagnote.core.domain.tracks.service.dto.TrackData;
import com.tagnote.core.domain.tracks.service.dto.response.SearchTrackList;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class SearchTracksResponse {
    private List<TrackData> trackDataList;
    private Integer totalCount;

    public static SearchTracksResponse from(SearchTrackList searchTrackList) {
        return builder()
                .trackDataList(searchTrackList.getTrackDataList())
                .totalCount(searchTrackList.getTotalCount())
                .build();
    }
}
