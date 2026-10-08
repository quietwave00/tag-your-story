package com.tagnote.api.domain.tracks.dto.response;

import com.tagnote.application.catalog.search.model.TrackSearchItem;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "Spotify 트랙 검색 결과 항목")
public class SearchTrackItemResponse {

    @Schema(description = "Spotify track id", example = "4u7EnebtmKWzUH433cf5Qv", requiredMode = Schema.RequiredMode.REQUIRED)
    private String trackId;

    @Schema(description = "첫 번째 아티스트 이름", example = "Queen", requiredMode = Schema.RequiredMode.REQUIRED)
    private String artistName;

    @Schema(description = "트랙 제목", example = "Bohemian Rhapsody", requiredMode = Schema.RequiredMode.REQUIRED)
    private String title;

    @Schema(description = "앨범 이름", example = "A Night At The Opera", requiredMode = Schema.RequiredMode.REQUIRED)
    private String albumName;

    @Schema(description = "앨범 첫 이미지 URL. 이미지가 없으면 NO_IMAGE", example = "https://i.scdn.co/image/example", requiredMode = Schema.RequiredMode.REQUIRED)
    private String imageUrl;

    public static SearchTrackItemResponse from(TrackSearchItem item) {
        return SearchTrackItemResponse.builder()
                .trackId(item.getSpotifyTrackId())
                .artistName(item.getArtistName())
                .title(item.getTitle())
                .albumName(item.getAlbumName())
                .imageUrl(item.getImageUrl())
                .build();
    }
}
