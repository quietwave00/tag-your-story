package com.tagnote.application.enrichment.port;

public interface EnrichmentConflictTranslator {

    RuntimeException translate(RuntimeException failure);
}
