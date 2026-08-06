package com.tagnote.api.domain.tracks.dto.response;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.tagnote.core.domain.tracks.service.dto.response.RankingList;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RankingListResponse {
    @JsonUnwrapped
    RankingList rankingList;

    public static RankingListResponse from(RankingList rankingList) {
        return RankingListResponse.builder()
                .rankingList(rankingList)
                .build();
    }
}
