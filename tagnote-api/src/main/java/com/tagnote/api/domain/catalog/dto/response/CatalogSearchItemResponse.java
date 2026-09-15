package com.tagnote.api.domain.catalog.dto.response;

import com.tagnote.application.catalog.search.model.CatalogSearchItem;
import com.tagnote.application.catalog.search.model.CatalogSearchSubjectType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CatalogSearchItemResponse {

    @Schema(description = "검색 결과 종류", example = "TRACK", requiredMode = Schema.RequiredMode.REQUIRED)
    private CatalogSearchSubjectType subjectType;

    @Schema(description = "subjectType에 해당하는 Spotify ID", example = "4u7EnebtmKWzUH433cf5Qv", requiredMode = Schema.RequiredMode.REQUIRED)
    private String spotifyId;

    @Schema(description = "Track 제목, Album 제목 또는 Artist 이름", example = "Bohemian Rhapsody", requiredMode = Schema.RequiredMode.REQUIRED)
    private String title;

    @Schema(description = "대표 Artist 이름. ARTIST 결과에서는 null", example = "Queen", nullable = true)
    private String artistName;

    @Schema(description = "Track이 수록된 Album 이름. ALBUM과 ARTIST 결과에서는 null", example = "A Night At The Opera", nullable = true)
    private String albumName;

    @Schema(description = "Spotify 이미지 URL. 이미지가 없으면 NO_IMAGE", example = "https://i.scdn.co/image/example", requiredMode = Schema.RequiredMode.REQUIRED)
    private String imageUrl;

    public static CatalogSearchItemResponse from(CatalogSearchItem item) {
        return CatalogSearchItemResponse.builder()
                .subjectType(item.getSubjectType())
                .spotifyId(item.getSpotifyId())
                .title(item.getTitle())
                .artistName(item.getArtistName())
                .albumName(item.getAlbumName())
                .imageUrl(item.getImageUrl())
                .build();
    }
}
