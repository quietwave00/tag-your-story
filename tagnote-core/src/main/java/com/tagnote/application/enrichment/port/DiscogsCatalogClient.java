package com.tagnote.application.enrichment.port;

import com.tagnote.application.enrichment.matching.model.DiscogsCatalogData.AlbumCandidate;
import com.tagnote.application.enrichment.matching.model.DiscogsCatalogData.AlbumDetails;
import com.tagnote.application.enrichment.matching.model.DiscogsCatalogData.EntityType;

import java.util.List;

public interface DiscogsCatalogClient {

    List<AlbumCandidate> searchAlbums(
            String albumTitle,
            List<String> artists,
            EntityType type
    );

    AlbumDetails getAlbum(EntityType type, long id);
}
