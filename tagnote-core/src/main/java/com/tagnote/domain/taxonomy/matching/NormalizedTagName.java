package com.tagnote.domain.taxonomy.matching;

import java.util.Objects;

public record NormalizedTagName(String value) {

    public NormalizedTagName {
        Objects.requireNonNull(value, "Normalized tag name must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("Normalized tag name must not be blank");
        }
    }
}
