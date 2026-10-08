package com.tagnote.application.enrichment.port;

import com.tagnote.application.enrichment.matching.model.LastFmCatalogData.TopTags;

public interface LastFmCatalogClient {

    TopTags getTrackTopTags(String artist, String track);

    TopTags getAlbumTopTags(String artist, String album);
}
