package com.tagnote.domain.resolution;

import java.util.Objects;

public record DirectTagEvidence(long tagId, double confidence, ResolutionReason reason) {

    public DirectTagEvidence {
        if (tagId <= 0) {
            throw new IllegalArgumentException("Tag ID must be positive");
        }
        if (!Double.isFinite(confidence) || confidence < 0.0 || confidence > 1.0) {
            throw new IllegalArgumentException("Confidence must be between 0.0 and 1.0");
        }
        Objects.requireNonNull(reason, "Resolution reason must not be null");
        if (reason != ResolutionReason.AUTO && reason != ResolutionReason.INHERITED_FROM_ALBUM) {
            throw new IllegalArgumentException("Evidence reason must be AUTO or INHERITED_FROM_ALBUM");
        }
    }

    public DirectTagEvidence(long tagId, double confidence) {
        this(tagId, confidence, ResolutionReason.AUTO);
    }

    public static DirectTagEvidence inheritedFromAlbum(long tagId, double confidence) {
        return new DirectTagEvidence(tagId, confidence, ResolutionReason.INHERITED_FROM_ALBUM);
    }

    public boolean isDirect() {
        return reason == ResolutionReason.AUTO;
    }
}
