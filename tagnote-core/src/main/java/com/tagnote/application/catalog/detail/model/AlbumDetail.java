package com.tagnote.application.catalog.detail.model;

import com.tagnote.application.catalog.importer.model.ImportedAlbum;

import java.util.List;
import java.util.Objects;

public record AlbumDetail(ImportedAlbum album, List<SystemTagDetail> systemTags,
                          List<PreviewTagDetail> previewTags, TagDisplayStatus tagDisplayStatus) {
    public AlbumDetail {
        Objects.requireNonNull(album, "Album must not be null");
        systemTags = List.copyOf(systemTags);
        previewTags = List.copyOf(previewTags);
        Objects.requireNonNull(tagDisplayStatus, "Tag display status must not be null");
        if (!systemTags.isEmpty() && !previewTags.isEmpty()) {
            throw new IllegalArgumentException("Confirmed and preview tags must not be mixed");
        }
    }

    public AlbumDetail(ImportedAlbum album, List<SystemTagDetail> systemTags) {
        this(album, List.copyOf(systemTags), List.of(), systemTags.isEmpty()
                ? TagDisplayStatus.EMPTY : TagDisplayStatus.CONFIRMED);
    }

    public AlbumDetail withPreview(List<PreviewTagDetail> preview) {
        if (!systemTags.isEmpty() || preview.isEmpty()) return this;
        return new AlbumDetail(album, systemTags, List.copyOf(preview), TagDisplayStatus.PREVIEW);
    }
}
