package com.tagnote.api.domain.catalog.dto.response;

import com.tagnote.application.catalog.detail.model.SpotifyAlbumSummary;
import com.tagnote.application.catalog.detail.model.SpotifyCatalogPage;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Spotify Artist의 Album 목록. Spotify 기본 포함 범위를 유지")
public record CatalogArtistAlbumsResponse(
        @Schema(description = "현재 page의 Album 후보, 최대 10개", requiredMode = Schema.RequiredMode.REQUIRED) List<AlbumItem> items,
        @Schema(description = "Spotify가 반환한 전체 결과 수", requiredMode = Schema.RequiredMode.REQUIRED) long totalCount
) {
    public static CatalogArtistAlbumsResponse from(SpotifyCatalogPage<SpotifyAlbumSummary> page) {
        return new CatalogArtistAlbumsResponse(page.items().stream().map(AlbumItem::from).toList(), page.totalCount());
    }

    public record AlbumItem(
            @Schema(description = "Spotify Album ID", requiredMode = Schema.RequiredMode.REQUIRED) String spotifyId,
            @Schema(description = "Album 제목", requiredMode = Schema.RequiredMode.REQUIRED) String title,
            @Schema(description = "이미지 URL. 이미지가 없으면 NO_IMAGE", requiredMode = Schema.RequiredMode.REQUIRED) String imageUrl,
            @Schema(description = "Spotify 발매일. 연도 또는 연월만 포함될 수 있음", nullable = true) String releaseDate
    ) {
        public static AlbumItem from(SpotifyAlbumSummary album) {
            return new AlbumItem(album.spotifyId(), album.title(), album.imageUrl(), album.releaseDate());
        }
    }
}
