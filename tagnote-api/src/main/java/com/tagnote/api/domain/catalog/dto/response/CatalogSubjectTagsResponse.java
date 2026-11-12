package com.tagnote.api.domain.catalog.dto.response;

import com.tagnote.api.domain.tracks.dto.response.PreviewTagResponse;
import com.tagnote.api.domain.tracks.dto.response.SystemTagResponse;
import com.tagnote.application.catalog.detail.model.SubjectTags;
import com.tagnote.application.catalog.detail.model.TagDisplayStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Track 또는 Album subject의 태그. 상세 메타데이터는 포함하지 않음")
public record CatalogSubjectTagsResponse(
        @Schema(description = "HIDDEN을 제외한 확정 System Tag. 점수 내림차순, 동점이면 tagId 오름차순",
                requiredMode = Schema.RequiredMode.REQUIRED)
        List<SystemTagResponse> systemTags,
        @Schema(description = "확정 태그가 없을 때만 제공하는 검증 전 원본 태그. 최대 5개",
                requiredMode = Schema.RequiredMode.REQUIRED)
        List<PreviewTagResponse> previewTags,
        @Schema(description = "CONFIRMED, PREVIEW 또는 EMPTY", requiredMode = Schema.RequiredMode.REQUIRED)
        TagDisplayStatus tagDisplayStatus
) {
    public static CatalogSubjectTagsResponse from(SubjectTags detail) {
        return new CatalogSubjectTagsResponse(
                detail.systemTags().stream().map(SystemTagResponse::from).toList(),
                detail.previewTags().stream().map(PreviewTagResponse::from).toList(),
                detail.tagDisplayStatus());
    }
}
