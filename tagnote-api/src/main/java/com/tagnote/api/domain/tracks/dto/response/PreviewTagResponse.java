package com.tagnote.api.domain.tracks.dto.response;

import com.tagnote.application.catalog.detail.model.PreviewTagDetail;
import com.tagnote.domain.enrichment.observation.ExternalTagSource;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "검증 전 외부 원본 태그. 확정 System Tag ID나 점수는 없음")
public record PreviewTagResponse(
        @Schema(description = "외부 제공자의 원본 표시 이름", example = "Dream Pop") String name,
        @Schema(description = "태그를 제공한 출처", example = "LASTFM") ExternalTagSource source
) {
    public static PreviewTagResponse from(PreviewTagDetail detail) {
        return new PreviewTagResponse(detail.name(), detail.source());
    }
}
