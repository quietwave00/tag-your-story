package com.tagnote.api.domain.catalog.dto.response;

import com.tagnote.api.domain.catalog.dto.request.CatalogImportSubjectType;
import com.tagnote.api.domain.tracks.dto.response.CatalogTrackResponse;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "subjectType에 해당하는 track 또는 album 한 가지만 채워지는 import 결과")
public record CatalogImportResponse(
        CatalogImportSubjectType subjectType,
        @Schema(nullable = true) CatalogTrackResponse track,
        @Schema(nullable = true) CatalogAlbumImportResponse album
) {
    public static CatalogImportResponse track(CatalogTrackResponse track) {
        return new CatalogImportResponse(CatalogImportSubjectType.TRACK, track, null);
    }

    public static CatalogImportResponse album(CatalogAlbumImportResponse album) {
        return new CatalogImportResponse(CatalogImportSubjectType.ALBUM, null, album);
    }
}
