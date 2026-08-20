package com.tagnote.api.domain.tracks.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Spotify Track Catalog import 요청")
public class ImportTrackRequest {

    @NotBlank(message = "spotifyTrackId는 비어 있을 수 없습니다.")
    @Schema(
            description = "검색 결과에서 선택한 Spotify track id",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "4u7EnebtmKWzUH433cf5Qv"
    )
    private String spotifyTrackId;
}
