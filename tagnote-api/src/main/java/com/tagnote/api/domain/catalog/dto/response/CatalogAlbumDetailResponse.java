package com.tagnote.api.domain.catalog.dto.response;

import com.tagnote.application.catalog.detail.model.CatalogReleaseType;
import com.tagnote.application.catalog.detail.model.SpotifyAlbumDetail;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Spotify Album 상세 메타데이터. 조회 시 Catalog에 저장하지 않음")
public record CatalogAlbumDetailResponse(
        @Schema(description = "Spotify Album ID", requiredMode = Schema.RequiredMode.REQUIRED) String spotifyId,
        @Schema(description = "Album 제목", requiredMode = Schema.RequiredMode.REQUIRED) String title,
        @Schema(description = "표지 이미지 URL. 이미지가 없으면 NO_IMAGE", requiredMode = Schema.RequiredMode.REQUIRED) String imageUrl,
        @Schema(description = "Spotify 발매일. 연도 또는 연월만 제공될 수 있음", nullable = true) String releaseDate,
        @Schema(description = "Album의 전체 Track 수", nullable = true) Integer totalTracks,
        @Schema(description = "발매 유형. Spotify compilation은 COMPILATION, single은 SINGLE_EP. album은 6곡 이하 또는 총 재생 시간 30분 미만이면 SINGLE_EP. 판정할 수 없으면 null", nullable = true,
                allowableValues = {"ALBUM", "COMPILATION", "SINGLE_EP"}) CatalogReleaseType releaseType,
        @Schema(description = "Spotify 표시 순서의 참여 Artist", requiredMode = Schema.RequiredMode.REQUIRED) List<ArtistCreditResponse> artists
) {
    public static CatalogAlbumDetailResponse from(SpotifyAlbumDetail detail) {
        return new CatalogAlbumDetailResponse(detail.spotifyId(), detail.title(), detail.imageUrl(),
                detail.releaseDate(), detail.totalTracks(), detail.releaseType(), detail.artists().stream()
                .map(credit -> new ArtistCreditResponse(credit.spotifyId(), credit.name())).toList());
    }

    public record ArtistCreditResponse(
            @Schema(description = "Spotify Artist ID", requiredMode = Schema.RequiredMode.REQUIRED) String spotifyId,
            @Schema(description = "Artist 이름", requiredMode = Schema.RequiredMode.REQUIRED) String name
    ) {
    }
}
