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

    public CollectedExternalTags merge(CollectedExternalTags other) {
        Objects.requireNonNull(other, "Collected tags to merge must not be null");
        return new CollectedExternalTags(
                java.util.stream.Stream.concat(albumInputs.stream(), other.albumInputs.stream()).toList(),
                java.util.stream.Stream.concat(trackInputs.stream(), other.trackInputs.stream()).toList()
        );
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
