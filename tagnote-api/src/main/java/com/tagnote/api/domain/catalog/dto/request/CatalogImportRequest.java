package com.tagnote.api.domain.catalog.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Catalog 검색 결과에서 선택한 subjectType과 spotifyId")
public record CatalogImportRequest(
        @NotNull @Schema(description = "가져올 대상. ARTIST는 지원하지 않음", example = "ALBUM", requiredMode = Schema.RequiredMode.REQUIRED)
        CatalogImportSubjectType subjectType,
        @NotBlank @Schema(description = "선택한 대상의 Spotify ID", example = "6i6folBtxKV28WX3msQ4FE", requiredMode = Schema.RequiredMode.REQUIRED)
        String spotifyId
) {}
