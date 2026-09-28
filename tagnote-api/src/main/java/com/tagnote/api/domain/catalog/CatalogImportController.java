package com.tagnote.api.domain.catalog;

import com.tagnote.api.domain.catalog.dto.request.CatalogImportRequest;
import com.tagnote.api.domain.catalog.dto.response.CatalogAlbumImportResponse;
import com.tagnote.api.domain.catalog.dto.response.CatalogImportResponse;
import com.tagnote.api.domain.tracks.dto.response.CatalogTrackResponse;
import com.tagnote.application.catalog.selection.AlbumSelectionService;
import com.tagnote.application.catalog.selection.TrackSelectionService;
import com.tagnote.core.utils.api.ApiResult;
import com.tagnote.core.utils.api.ApiUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/catalog")
@RequiredArgsConstructor
public class CatalogImportController implements CatalogImportApi {
    private final TrackSelectionService tracks;
    private final AlbumSelectionService albums;

    @PostMapping("/import")
    @Override
    public ApiResult<CatalogImportResponse> importCatalog(@Valid @RequestBody CatalogImportRequest request) {
        return ApiUtils.success(switch (request.subjectType()) {
            case TRACK -> CatalogImportResponse.track(
                    CatalogTrackResponse.from(tracks.select(request.spotifyId())));
            case ALBUM -> CatalogImportResponse.album(
                    CatalogAlbumImportResponse.from(albums.select(request.spotifyId())));
        });
    }
}
