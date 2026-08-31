package com.tagnote.core.domain.usertag.name;

import java.util.Objects;

public record NormalizedUserTagName(String value) {

    public NormalizedUserTagName {
        Objects.requireNonNull(value, "Normalized user tag name must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("Normalized user tag name must not be blank");
        }
    }
}
