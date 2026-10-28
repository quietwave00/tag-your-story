package com.tagnote.api.domain.catalog.dto.response;

import com.tagnote.application.catalog.detail.model.SpotifyAlbumTrack;
import com.tagnote.application.catalog.detail.model.SpotifyCatalogPage;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Spotify Album의 Track 목록. Spotify 디스크·트랙 순서를 유지")
public record CatalogAlbumTracksResponse(
        @Schema(description = "현재 page의 Track 후보, 최대 10개", requiredMode = Schema.RequiredMode.REQUIRED) List<TrackItem> items,
        @Schema(description = "Spotify가 반환한 전체 결과 수", requiredMode = Schema.RequiredMode.REQUIRED) long totalCount
) {
    public static CatalogAlbumTracksResponse from(SpotifyCatalogPage<SpotifyAlbumTrack> page) {
        return new CatalogAlbumTracksResponse(page.items().stream().map(TrackItem::from).toList(), page.totalCount());
    }

    public record TrackItem(
            @Schema(description = "Spotify Track ID", requiredMode = Schema.RequiredMode.REQUIRED) String spotifyId,
            @Schema(description = "Track 제목", requiredMode = Schema.RequiredMode.REQUIRED) String title,
            @Schema(description = "대표 Artist 이름. credit이 없으면 null", nullable = true) String artistName,
            @Schema(description = "디스크 번호", nullable = true) Integer discNumber,
            @Schema(description = "디스크 내 Track 번호", nullable = true) Integer trackNumber,
            @Schema(description = "Track 길이(ms)", nullable = true) Integer durationMs
    ) {
        public static TrackItem from(SpotifyAlbumTrack track) {
            return new TrackItem(track.spotifyId(), track.title(), track.artistName(),
                    track.discNumber(), track.trackNumber(), track.durationMs());
        }
    }
}
