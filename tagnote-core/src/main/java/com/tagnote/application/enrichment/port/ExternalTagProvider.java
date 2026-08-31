package com.tagnote.application.enrichment.port;

import com.tagnote.application.catalog.importer.model.ImportedTrack;
import com.tagnote.application.enrichment.model.CollectedExternalTags;

public interface ExternalTagProvider {

    CollectedExternalTags collect(ImportedTrack track);
}
