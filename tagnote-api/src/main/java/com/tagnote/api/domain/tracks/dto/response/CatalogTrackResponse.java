package com.tagnote.api.domain.tracks.dto.response;

import com.tagnote.application.catalog.importer.model.ImportedTrack;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@Schema(description = "Import된 내부 Catalog Track")
public class CatalogTrackResponse {

    @Schema(description = "내부 Track ID", example = "10")
    private Long catalogTrackId;

    @Schema(description = "Spotify track id", example = "4u7EnebtmKWzUH433cf5Qv")
    private String spotifyTrackId;

    @Schema(description = "Track 제목", example = "Track title")
    private String title;

    @Schema(description = "ISRC. Spotify가 제공하지 않으면 null", example = "USRC17607839", nullable = true)
    private String isrc;

    @Schema(description = "Track 재생 시간(ms)", example = "240000")
    private Integer durationMs;

    @Schema(description = "Spotify 순서가 보존된 Track Artist 전체 목록")
    private List<CatalogArtistResponse> artists;

    private CatalogAlbumResponse album;

    public static CatalogTrackResponse from(ImportedTrack track) {
        return CatalogTrackResponse.builder()
                .catalogTrackId(track.getCatalogTrackId())
                .spotifyTrackId(track.getSpotifyTrackId())
                .title(track.getTitle())
                .isrc(track.getIsrc())
                .durationMs(track.getDurationMs())
                .artists(track.getArtists().stream().map(CatalogArtistResponse::from).toList())
                .album(CatalogAlbumResponse.from(track.getAlbum()))
                .build();
    }
}
