package com.tagnote.api.domain.tracks.dto.response;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.tagnote.core.domain.tracks.service.dto.response.RankingList;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "검색 횟수 기준 상위 5개 키워드 응답")
public class RankingListResponse {
    @JsonUnwrapped
    @Schema(description = "keywordList 필드로 펼쳐지는 검색어 랭킹")
    RankingList rankingList;

    public static RankingListResponse from(RankingList rankingList) {
        return RankingListResponse.builder()
                .rankingList(rankingList)
                .build();
    }
}
