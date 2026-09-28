package com.tagnote.api.domain.catalog.dto.response;

import com.tagnote.api.domain.tracks.dto.response.CatalogAlbumResponse;
import com.tagnote.api.domain.tracks.dto.response.PreviewTagResponse;
import com.tagnote.api.domain.tracks.dto.response.SystemTagResponse;
import com.tagnote.application.catalog.detail.model.AlbumDetail;
import com.tagnote.application.catalog.detail.model.TagDisplayStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Album Catalog와 Album subject에 속하는 태그")
public record CatalogAlbumImportResponse(
        CatalogAlbumResponse album,
        @Schema(description = "ALBUM resolved System Tag. HIDDEN 제외") List<SystemTagResponse> systemTags,
        @Schema(description = "확정 태그가 없을 때의 검증 전 ALBUM 태그, 최대 5개") List<PreviewTagResponse> previewTags,
        TagDisplayStatus tagDisplayStatus
) {
    public static CatalogAlbumImportResponse from(AlbumDetail detail) {
        return new CatalogAlbumImportResponse(
                CatalogAlbumResponse.from(detail.album()),
                detail.systemTags().stream().map(SystemTagResponse::from).toList(),
                detail.previewTags().stream().map(PreviewTagResponse::from).toList(),
                detail.tagDisplayStatus());
    }
}
