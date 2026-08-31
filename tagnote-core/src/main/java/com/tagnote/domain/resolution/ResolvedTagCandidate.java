package com.tagnote.domain.resolution;

import java.util.Objects;

public record ResolvedTagCandidate(long tagId, double score, ResolutionReason reason) {

    public ResolvedTagCandidate {
        if (tagId <= 0) {
            throw new IllegalArgumentException("Tag ID must be positive");
        }
        if (!Double.isFinite(score) || score < 0.0 || score > 1.0) {
            throw new IllegalArgumentException("Resolved score must be between 0.0 and 1.0");
        }
        Objects.requireNonNull(reason, "Resolution reason must not be null");
    }

    public static ResolvedTagCandidate automatic(long tagId, double score) {
        return new ResolvedTagCandidate(tagId, score, ResolutionReason.AUTO);
    }

    public static ResolvedTagCandidate inheritedFromAlbum(long tagId, double score) {
        return new ResolvedTagCandidate(tagId, score, ResolutionReason.INHERITED_FROM_ALBUM);
    }
}
