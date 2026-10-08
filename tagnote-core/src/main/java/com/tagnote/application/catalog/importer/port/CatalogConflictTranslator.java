package com.tagnote.application.catalog.importer.port;

public interface CatalogConflictTranslator {

    RuntimeException translate(RuntimeException failure);
}
