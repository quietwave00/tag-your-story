package com.tagnote.api.domain.catalog.dto.response;

import com.tagnote.application.catalog.detail.model.SpotifyArtistDetail;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Spotify Artist 상세 메타데이터. 팔로워 수는 포함하지 않음")
public record CatalogArtistDetailResponse(
        @Schema(description = "Spotify Artist ID", requiredMode = Schema.RequiredMode.REQUIRED) String spotifyId,
        @Schema(description = "Artist 이름", requiredMode = Schema.RequiredMode.REQUIRED) String name,
        @Schema(description = "Artist 이미지 URL. 이미지가 없으면 NO_IMAGE", requiredMode = Schema.RequiredMode.REQUIRED) String imageUrl,
        @Schema(description = "Spotify Artist 장르. 장르 정보가 없으면 빈 목록", requiredMode = Schema.RequiredMode.REQUIRED) List<String> genres
) {
    public static CatalogArtistDetailResponse from(SpotifyArtistDetail detail) {
        return new CatalogArtistDetailResponse(detail.spotifyId(), detail.name(), detail.imageUrl(), detail.genres());
    }
}
