package com.tagnote.application.catalog.importer.model;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ImportedTrack {

    private final Long catalogTrackId;
    private final String spotifyTrackId;
    private final String musicBrainzRecordingId;
    private final String title;
    private final String isrc;
    private final Integer durationMs;
    private final List<ImportedArtist> artists;
    private final ImportedAlbum album;

    public static ImportedTrack of(
            Long catalogTrackId,
            String spotifyTrackId,
            String musicBrainzRecordingId,
            String title,
            String isrc,
            Integer durationMs,
            List<ImportedArtist> artists,
            ImportedAlbum album
    ) {
        return ImportedTrack.builder()
                .catalogTrackId(catalogTrackId)
                .spotifyTrackId(spotifyTrackId)
                .musicBrainzRecordingId(musicBrainzRecordingId)
                .title(title)
                .isrc(isrc)
                .durationMs(durationMs)
                .artists(List.copyOf(artists))
                .album(album)
                .build();
    }

    public ImportedTrack withMusicBrainzRecordingId(String recordingId) {
        String updatedRecordingId = recordingId == null ? musicBrainzRecordingId : recordingId;
        if (java.util.Objects.equals(updatedRecordingId, musicBrainzRecordingId)) {
            return this;
        }
        return ImportedTrack.of(
                catalogTrackId,
                spotifyTrackId,
                updatedRecordingId,
                title,
                isrc,
                durationMs,
                artists,
                album
        );
    }
}
