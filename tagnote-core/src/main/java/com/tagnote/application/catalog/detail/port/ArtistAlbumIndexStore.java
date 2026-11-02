package com.tagnote.application.catalog.detail.port;

import com.tagnote.application.catalog.detail.model.ArtistAlbumIndex;

public interface ArtistAlbumIndexStore {
    ArtistAlbumIndex find(String artistId);

    void save(String artistId, ArtistAlbumIndex index);
}
