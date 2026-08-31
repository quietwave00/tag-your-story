package com.tagnote.api.domain.tracks.dto.response;

import com.tagnote.application.catalog.detail.model.SystemTagDetail;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "Track에 계산된 System Tag")
public class SystemTagResponse {

    @Schema(description = "System Tag ID", example = "1")
    private long tagId;

    @Schema(description = "System Tag 표시 이름", example = "Ambient")
    private String name;

    @Schema(description = "Resolver 계산 점수", example = "0.9", minimum = "0", maximum = "1")
    private double score;

    public static SystemTagResponse from(SystemTagDetail detail) {
        return SystemTagResponse.builder()
                .tagId(detail.tagId())
                .name(detail.name())
                .score(detail.score())
                .build();
    }
}
