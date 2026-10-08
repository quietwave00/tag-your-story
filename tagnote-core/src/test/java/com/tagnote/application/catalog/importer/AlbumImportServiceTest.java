package com.tagnote.application.catalog.importer;

import com.tagnote.application.catalog.importer.model.ImportedAlbum;
import com.tagnote.application.catalog.importer.model.SpotifyAlbumMetadata;
import com.tagnote.application.catalog.importer.port.SpotifyAlbumMetadataProvider;
import com.tagnote.application.catalog.importer.exception.CatalogDuplicateException;
import com.tagnote.application.catalog.importer.exception.CatalogDuplicateTarget;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class AlbumImportServiceTest {
    @Mock CatalogAlbumReadService reader;
    @Mock CatalogAlbumWriteService writer;
    @Mock SpotifyAlbumMetadataProvider spotify;
    @InjectMocks AlbumImportService service;

    @Test
    void 신규_album은_spotify_album_metadata로_생성한다() {
        SpotifyAlbumMetadata metadata = new SpotifyAlbumMetadata("album-1", "Album", 2024, List.of());
        ImportedAlbum created = ImportedAlbum.of(5L, "album-1", "Album", 2024, List.of());
        when(reader.findBySpotifyId("album-1")).thenReturn(Optional.empty());
        when(spotify.getAlbum("album-1")).thenReturn(metadata);
        when(writer.create(metadata)).thenReturn(created);

        assertThat(service.importAlbum("album-1")).isSameAs(created);
        verify(spotify).getAlbum("album-1");
    }

    @Test
    void 기존_album은_spotify_호출없이_재사용한다() {
        ImportedAlbum existing = ImportedAlbum.of(5L, "album-1", "Album", 2024, List.of());
        when(reader.findBySpotifyId("album-1")).thenReturn(Optional.of(existing));

        assertThat(service.importAlbum("album-1")).isSameAs(existing);
        verify(spotify, never()).getAlbum("album-1");
    }

    @Test
    void 동시_album_생성_충돌은_canonical_album을_반환한다() {
        SpotifyAlbumMetadata metadata = new SpotifyAlbumMetadata("album-1", "Album", 2024, List.of());
        ImportedAlbum canonical = ImportedAlbum.of(5L, "album-1", "Album", 2024, List.of());
        when(reader.findBySpotifyId("album-1")).thenReturn(Optional.empty(), Optional.of(canonical));
        when(spotify.getAlbum("album-1")).thenReturn(metadata);
        when(writer.create(metadata)).thenThrow(new CatalogDuplicateException(
                CatalogDuplicateTarget.ALBUM, new RuntimeException("duplicate")));

        assertThat(service.importAlbum("album-1")).isSameAs(canonical);
        verify(reader, times(2)).findBySpotifyId("album-1");
    }
}
