package com.tagnote.api.domain.catalog.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.tagnote.application.catalog.detail.model.ArtistAlbumListing;
import com.tagnote.application.catalog.detail.model.CatalogReleaseType;
import com.tagnote.application.catalog.detail.model.SpotifyAlbumSummary;
import com.tagnote.core.utils.api.ApiResult;
import com.tagnote.core.utils.api.ApiUtils;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@Schema(description = "Spotify Artist의 Album 목록. Spotify 기본 포함 범위를 유지")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CatalogArtistAlbumsResponse(
        @Schema(description = "현재 page의 Album 후보, 최대 10개. 각 항목의 releaseType은 nullable") List<AlbumItem> items,
        @Schema(description = "유형 지정 시 실제 수집된 항목 중 선택 유형의 건수. releaseType 생략 시 Spotify 원본 전체 건수") Long totalCount,
        @Schema(description = "명시적 유형 요청의 분류 완료 여부. 원본 페이지 요청에서는 생략") Boolean classificationComplete,
        @Schema(description = "분류되지 않은 항목 수. 원본 페이지 요청에서는 생략") Integer unclassifiedCount,
        @Schema(description = "인덱스 구축 중에는 BUILDING. 완료 응답에서는 생략", allowableValues = {"BUILDING"}) String status
) {
    public static ResponseEntity<ApiResult<CatalogArtistAlbumsResponse>> toHttpResponse(
            Optional<ArtistAlbumListing> listing) {
        if (listing.isEmpty()) {
            return ResponseEntity.status(HttpStatus.ACCEPTED).header("Retry-After", "1")
                    .body(ApiUtils.success(building()));
        }
        ArtistAlbumListing page = listing.get();
        CatalogArtistAlbumsResponse response = new CatalogArtistAlbumsResponse(
                page.items().stream().map(AlbumItem::from).toList(), page.totalCount(),
                page.classificationComplete(), page.unclassifiedCount(), null);
        return ResponseEntity.ok(ApiUtils.success(response));
    }

    public static CatalogArtistAlbumsResponse building() {
        return new CatalogArtistAlbumsResponse(null, null, null, null, "BUILDING");
    }

    public record AlbumItem(
            @Schema(description = "Spotify Album ID", requiredMode = Schema.RequiredMode.REQUIRED) String spotifyId,
            @Schema(description = "Album 제목", requiredMode = Schema.RequiredMode.REQUIRED) String title,
            @Schema(description = "이미지 URL. 이미지가 없으면 NO_IMAGE", requiredMode = Schema.RequiredMode.REQUIRED) String imageUrl,
            @Schema(description = "Spotify 발매일. 연도 또는 연월만 포함될 수 있음", nullable = true) String releaseDate,
            @Schema(description = "발매 유형. 분류에 필요한 metadata가 부족하거나 Track 조회가 실패하면 null", nullable = true,
                    allowableValues = {"ALBUM", "COMPILATION", "SINGLE_EP"}) CatalogReleaseType releaseType
    ) {
        public static AlbumItem from(SpotifyAlbumSummary album) {
            return new AlbumItem(album.spotifyId(), album.title(), album.imageUrl(), album.releaseDate(), album.releaseType());
        }
    }
}
