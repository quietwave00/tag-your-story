package com.tagnote.domain.resolution;

import com.tagnote.domain.taxonomy.tag.TagStatus;

import java.util.Objects;

public record CanonicalTagNode(long tagId, TagStatus status, Long mergedIntoTagId) {

    public CanonicalTagNode {
        if (tagId <= 0) {
            throw new IllegalArgumentException("Tag ID must be positive");
        }
        Objects.requireNonNull(status, "Tag status must not be null");
    }
}
