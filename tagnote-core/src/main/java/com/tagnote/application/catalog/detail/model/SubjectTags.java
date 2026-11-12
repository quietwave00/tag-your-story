package com.tagnote.application.catalog.detail.model;

import java.util.List;

public record SubjectTags(List<SystemTagDetail> systemTags,
                          List<PreviewTagDetail> previewTags,
                          TagDisplayStatus tagDisplayStatus) {
    public SubjectTags {
        systemTags = List.copyOf(systemTags);
        previewTags = List.copyOf(previewTags);
        if (!systemTags.isEmpty() && !previewTags.isEmpty()) {
            throw new IllegalArgumentException("Confirmed and preview tags must not be mixed");
        }
    }

    public static SubjectTags confirmed(List<SystemTagDetail> systemTags) {
        return new SubjectTags(systemTags, List.of(), systemTags.isEmpty()
                ? TagDisplayStatus.EMPTY : TagDisplayStatus.CONFIRMED);
    }

    public static SubjectTags preview(List<PreviewTagDetail> previewTags) {
        return new SubjectTags(List.of(), previewTags, previewTags.isEmpty()
                ? TagDisplayStatus.EMPTY : TagDisplayStatus.PREVIEW);
    }
}
