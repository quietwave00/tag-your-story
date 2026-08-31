package com.tagnote.application.enrichment.model;

import java.util.List;
import java.util.Objects;

public record CollectedExternalTags(
        List<ExternalTagInput> albumInputs,
        List<ExternalTagInput> trackInputs
) {

    public CollectedExternalTags {
        albumInputs = validateAndCopy(albumInputs, "Album inputs");
        trackInputs = validateAndCopy(trackInputs, "Track inputs");
    }

    public static CollectedExternalTags empty() {
        return new CollectedExternalTags(List.of(), List.of());
    }

    private static List<ExternalTagInput> validateAndCopy(
            List<ExternalTagInput> inputs,
            String fieldName
    ) {
        Objects.requireNonNull(inputs, fieldName + " must not be null");
        if (inputs.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException(fieldName + " must not contain null");
        }
        return List.copyOf(inputs);
    }
}
