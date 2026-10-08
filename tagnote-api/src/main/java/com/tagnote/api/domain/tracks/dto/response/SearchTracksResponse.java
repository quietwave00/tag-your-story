package com.tagnote.api.domain.tracks.dto.response;

import com.tagnote.application.catalog.search.model.TrackSearchResult;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@Schema(description = "트랙 검색 응답")
public class SearchTracksResponse {

    @Schema(description = "현재 페이지의 트랙 목록", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<SearchTrackItemResponse> trackDataList;

    @Schema(description = "Spotify 검색 결과 전체 개수", example = "42", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer totalCount;

    public static SearchTracksResponse from(TrackSearchResult searchResult) {
        return builder()
                .trackDataList(searchResult.getItems().stream()
                        .map(SearchTrackItemResponse::from)
                        .toList())
                .totalCount(searchResult.getTotalCount())
                .build();
    }
}
