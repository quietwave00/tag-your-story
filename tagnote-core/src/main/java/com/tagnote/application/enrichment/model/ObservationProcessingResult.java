package com.tagnote.application.enrichment.model;

import com.tagnote.domain.enrichment.observation.ExternalTagSource;

import java.util.List;

public record ObservationProcessingResult(
        int createdObservationCount,
        int reusedObservationCount,
        int matchedObservationCount,
        int newObservationCount,
        int createdAssertionCount,
        int reusedAssertionCount,
        List<NewObservation> newObservations
) {

    public ObservationProcessingResult {
        newObservations = List.copyOf(newObservations);
    }

    public ObservationProcessingResult(int createdObservationCount, int reusedObservationCount,
                                       int matchedObservationCount, int newObservationCount,
                                       int createdAssertionCount, int reusedAssertionCount) {
        this(createdObservationCount, reusedObservationCount, matchedObservationCount,
                newObservationCount, createdAssertionCount, reusedAssertionCount, List.of());
    }

    public record NewObservation(String rawName, String normalizedName, ExternalTagSource source) {}

    public static ObservationProcessingResult empty() {
        return new ObservationProcessingResult(0, 0, 0, 0, 0, 0);
    }
}
