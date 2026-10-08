package com.tagnote.core.domain.tracks.webclient.dto;

import lombok.Builder;
import lombok.Getter;
import se.michaelthelin.spotify.model_objects.specification.AlbumSimplified;
import se.michaelthelin.spotify.model_objects.specification.Artist;
import se.michaelthelin.spotify.model_objects.specification.Track;

@Getter
@Builder
public class SpotifyCatalogSearchInfo {

    private Track[] tracks;
    private AlbumSimplified[] albums;
    private Artist[] artists;
    private int trackTotalCount;
    private int albumTotalCount;
    private int artistTotalCount;

    public static SpotifyCatalogSearchInfo of(
            Track[] tracks,
            AlbumSimplified[] albums,
            Artist[] artists,
            int trackTotalCount,
            int albumTotalCount,
            int artistTotalCount
    ) {
        return SpotifyCatalogSearchInfo.builder()
                .tracks(tracks)
                .albums(albums)
                .artists(artists)
                .trackTotalCount(trackTotalCount)
                .albumTotalCount(albumTotalCount)
                .artistTotalCount(artistTotalCount)
                .build();
    }
}
