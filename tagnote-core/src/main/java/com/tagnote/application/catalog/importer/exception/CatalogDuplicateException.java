package com.tagnote.application.catalog.importer.exception;

import lombok.Getter;

@Getter
public class CatalogDuplicateException extends RuntimeException {

    private final CatalogDuplicateTarget target;

    public CatalogDuplicateException(CatalogDuplicateTarget target, Throwable cause) {
        super("Catalog " + target.name().toLowerCase() + " Spotify ID already exists", cause);
        this.target = target;
    }

    public boolean isParentConflict() {
        return target == CatalogDuplicateTarget.ARTIST || target == CatalogDuplicateTarget.ALBUM;
    }
}
