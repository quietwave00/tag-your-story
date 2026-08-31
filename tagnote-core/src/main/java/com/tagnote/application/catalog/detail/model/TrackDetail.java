package com.tagnote.application.catalog.detail.model;

import com.tagnote.application.catalog.importer.model.ImportedTrack;

import java.util.List;
import java.util.Objects;

public record TrackDetail(ImportedTrack track, List<SystemTagDetail> systemTags) {

    public TrackDetail {
        Objects.requireNonNull(track, "Track must not be null");
        Objects.requireNonNull(systemTags, "System tags must not be null");
        systemTags = List.copyOf(systemTags);
    }
}
