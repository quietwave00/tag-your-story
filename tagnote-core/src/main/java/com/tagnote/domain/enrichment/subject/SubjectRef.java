package com.tagnote.domain.enrichment.subject;

import com.tagnote.domain.catalog.album.AlbumEntity;
import com.tagnote.domain.catalog.track.TrackEntity;

import java.util.Objects;

public record SubjectRef(SubjectType type, long subjectId) {

    public SubjectRef {
        Objects.requireNonNull(type, "Subject type must not be null");
        if (subjectId <= 0) {
            throw new IllegalArgumentException("Subject ID must be positive");
        }
    }

    public static SubjectRef track(long trackId) {
        return new SubjectRef(SubjectType.TRACK, trackId);
    }

    public static SubjectRef track(TrackEntity track) {
        Objects.requireNonNull(track, "Track must not be null");
        return track(requirePersistedId(track.getTrackId(), "Track"));
    }

    public static SubjectRef album(long albumId) {
        return new SubjectRef(SubjectType.ALBUM, albumId);
    }

    public static SubjectRef album(AlbumEntity album) {
        Objects.requireNonNull(album, "Album must not be null");
        return album(requirePersistedId(album.getAlbumId(), "Album"));
    }

    private static long requirePersistedId(Long id, String subjectName) {
        if (id == null) {
            throw new IllegalArgumentException(subjectName + " must be persisted before creating a subject reference");
        }
        return id;
    }
}
