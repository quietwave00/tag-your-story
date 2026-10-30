package com.tagnote.api.domain.tracks.dto.response;

import com.tagnote.core.domain.tracks.service.dto.TrackData;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "Spotify Track 상세 메타데이터")
public class DetailTrackResponse {
    @Schema(description = "Spotify Track ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private String trackId;
    @Schema(description = "표시하는 첫 번째 Spotify Artist 이름", requiredMode = Schema.RequiredMode.REQUIRED)
    private String artistName;
    @Schema(description = "Spotify Track 제목", requiredMode = Schema.RequiredMode.REQUIRED)
    private String title;
    @Schema(description = "Spotify Album 이름", requiredMode = Schema.RequiredMode.REQUIRED)
    private String albumName;
    @Schema(description = "Album 이미지 URL. 없으면 NO_IMAGE", requiredMode = Schema.RequiredMode.REQUIRED)
    private String imageUrl;
    @Schema(description = "표시하는 첫 번째 Spotify Artist ID. 없으면 null", nullable = true)
    private String spotifyArtistId;
    @Schema(description = "Spotify Album ID. 없으면 null", nullable = true)
    private String spotifyAlbumId;

    public static DetailTrackResponse from(TrackData detailTrack) {
        return builder()
                .trackId(detailTrack.getTrackId())
                .title(detailTrack.getTitle())
                .artistName(detailTrack.getArtistName())
                .albumName(detailTrack.getAlbumName())
                .imageUrl(detailTrack.getImageUrl())
                .spotifyArtistId(detailTrack.getSpotifyArtistId())
                .spotifyAlbumId(detailTrack.getSpotifyAlbumId())
                .build();
    }
}
