package com.tagnote.application.catalog.detail.model;

import com.tagnote.application.catalog.importer.model.ImportedTrack;

import java.util.List;
import java.util.Objects;

public record TrackDetail(ImportedTrack track, List<SystemTagDetail> systemTags,
                          List<PreviewTagDetail> previewTags, TagDisplayStatus tagDisplayStatus) {

    public TrackDetail(ImportedTrack track, List<SystemTagDetail> systemTags) {
        this(track, systemTags, List.of(), systemTags.isEmpty()
                ? TagDisplayStatus.EMPTY : TagDisplayStatus.CONFIRMED);
    }

    public TrackDetail {
        Objects.requireNonNull(track, "Track must not be null");
        Objects.requireNonNull(systemTags, "System tags must not be null");
        systemTags = List.copyOf(systemTags);
        previewTags = List.copyOf(previewTags);
        Objects.requireNonNull(tagDisplayStatus, "Tag display status must not be null");
        if (!systemTags.isEmpty() && !previewTags.isEmpty()) {
            throw new IllegalArgumentException("Confirmed and preview tags must not be mixed");
        }
    }

    public TrackDetail withPreview(List<PreviewTagDetail> preview) {
        if (!systemTags.isEmpty() || preview.isEmpty()) return this;
        return new TrackDetail(track, systemTags, preview, TagDisplayStatus.PREVIEW);
    }
}
