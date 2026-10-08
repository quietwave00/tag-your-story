package com.tagnote.api.domain.tracks.dto.response;

import com.tagnote.application.catalog.importer.model.ImportedAlbum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@Schema(description = "Import된 내부 Catalog Album")
public class CatalogAlbumResponse {

    @Schema(description = "내부 Album ID", example = "7")
    private Long albumId;

    @Schema(description = "Spotify album id", example = "spotify-album-id")
    private String spotifyAlbumId;

    @Schema(description = "Album 제목", example = "Album")
    private String title;

    @Schema(description = "발매 연도. Spotify release date가 없으면 null", example = "2024", nullable = true)
    private Integer releaseYear;

    @Schema(description = "Spotify 순서가 보존된 Album Artist 전체 목록")
    private List<CatalogArtistResponse> artists;

    public static CatalogAlbumResponse from(ImportedAlbum album) {
        return CatalogAlbumResponse.builder()
                .albumId(album.getAlbumId())
                .spotifyAlbumId(album.getSpotifyAlbumId())
                .title(album.getTitle())
                .releaseYear(album.getReleaseYear())
                .artists(album.getArtists().stream().map(CatalogArtistResponse::from).toList())
                .build();
    }
}
