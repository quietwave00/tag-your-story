package com.tagnote.application.board.model;

import com.tagnote.domain.enrichment.subject.SubjectType;

import java.util.Objects;

public record BoardSubject(SubjectType type, long catalogId, String spotifyId) {

    public BoardSubject {
        Objects.requireNonNull(type, "Board subject type must not be null");
        if (type != SubjectType.TRACK && type != SubjectType.ALBUM) {
            throw new IllegalArgumentException("Board subject must be TRACK or ALBUM");
        }
        if (catalogId <= 0) {
            throw new IllegalArgumentException("Board subject catalog id must be positive");
        }
        if (spotifyId == null || spotifyId.isBlank()) {
            throw new IllegalArgumentException("Board subject Spotify id must not be blank");
        }
    }

    public static BoardSubject track(long catalogId, String spotifyId) {
        return new BoardSubject(SubjectType.TRACK, catalogId, spotifyId);
    }

    public static BoardSubject album(long catalogId, String spotifyId) {
        return new BoardSubject(SubjectType.ALBUM, catalogId, spotifyId);
    }
}
