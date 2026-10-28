package com.tagnote.api.domain.catalog.dto.response;

import com.tagnote.application.catalog.search.model.CatalogScreenSearchResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "검색 화면의 타입별 독립 페이지. 각 타입은 현재 page 최대 10개를 반환")
public record CatalogScreenSearchResponse(
        @Schema(description = "Track 결과", requiredMode = Schema.RequiredMode.REQUIRED) Section tracks,
        @Schema(description = "Album 결과", requiredMode = Schema.RequiredMode.REQUIRED) Section albums,
        @Schema(description = "Artist 결과", requiredMode = Schema.RequiredMode.REQUIRED) Section artists
) {
    public static CatalogScreenSearchResponse from(CatalogScreenSearchResult result) {
        return new CatalogScreenSearchResponse(Section.from(result.tracks()), Section.from(result.albums()),
                Section.from(result.artists()));
    }

    public record Section(
            @Schema(description = "현재 타입 page의 후보, 최대 10개", requiredMode = Schema.RequiredMode.REQUIRED)
            List<CatalogSearchItemResponse> items,
            @Schema(description = "Spotify가 window 첫 요청에서 반환한 전체 건수. 검색 요청 사이 변할 수 있는 참고 값",
                    requiredMode = Schema.RequiredMode.REQUIRED) long totalCount,
            @Schema(description = "다음 page 조회 가능성. window 끝이 10개로 찼다면 다음 요청에서 빈 page가 나올 수 있음",
                    requiredMode = Schema.RequiredMode.REQUIRED) boolean hasNext
    ) {
        private static Section from(CatalogScreenSearchResult.Section section) {
            return new Section(section.items().stream().map(CatalogSearchItemResponse::from).toList(),
                    section.totalCount(), section.hasNext());
        }
    }
}
