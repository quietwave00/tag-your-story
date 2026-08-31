package com.tagnote.application.enrichment.model;

public record ObservationProcessingResult(
        int createdObservationCount,
        int reusedObservationCount,
        int matchedObservationCount,
        int newObservationCount,
        int createdAssertionCount,
        int reusedAssertionCount
) {

    public static ObservationProcessingResult empty() {
        return new ObservationProcessingResult(0, 0, 0, 0, 0, 0);
    }
}
