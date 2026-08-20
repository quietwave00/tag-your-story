package com.tagnote.api.domain.tracks.dto.response;

import com.tagnote.application.catalog.importer.model.ImportedArtist;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "Catalog Artist credit")
public class CatalogArtistResponse {

    @Schema(description = "내부 Artist ID", example = "3")
    private Long artistId;

    @Schema(description = "Spotify artist id", example = "spotify-artist-id")
    private String spotifyArtistId;

    @Schema(description = "Artist 표시 이름", example = "Artist A")
    private String name;

    @Schema(description = "Spotify Artist 배열의 0-based 표시 순서. 0은 대표 Artist", example = "0")
    private int position;

    public static CatalogArtistResponse from(ImportedArtist artist) {
        return CatalogArtistResponse.builder()
                .artistId(artist.getArtistId())
                .spotifyArtistId(artist.getSpotifyArtistId())
                .name(artist.getName())
                .position(artist.getPosition())
                .build();
    }
}
