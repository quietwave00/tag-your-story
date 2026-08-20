package com.tagnote.application.catalog.importer;

import com.tagnote.application.catalog.importer.model.ImportedAlbum;
import com.tagnote.application.catalog.importer.model.ImportedTrack;
import com.tagnote.application.catalog.importer.model.SpotifyTrackMetadata;
import com.tagnote.application.catalog.importer.port.SpotifyTrackMetadataProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrackImportServiceTest {

    @Mock
    private CatalogTrackReadService catalogTrackReadService;

    @Mock
    private CatalogWriteService catalogWriteService;

    @Mock
    private SpotifyTrackMetadataProvider spotifyTrackMetadataProvider;

    @InjectMocks
    private TrackImportService trackImportService;

    @Test
    void 이미_Import된_Track은_Spotify와_write를_호출하지_않는다() {
        ImportedTrack existing = importedTrack();
        when(catalogTrackReadService.findBySpotifyId("track-1")).thenReturn(Optional.of(existing));

        ImportedTrack result = trackImportService.importTrack("track-1");

        assertThat(result).isSameAs(existing);
        verify(spotifyTrackMetadataProvider, never()).getTrack("track-1");
        verify(catalogWriteService, never()).upsert(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void 최초_Import는_Spotify_조회_후_write하고_저장_결과를_읽는다() {
        SpotifyTrackMetadata metadata = metadata();
        ImportedTrack imported = importedTrack();
        when(catalogTrackReadService.findBySpotifyId("track-1")).thenReturn(Optional.empty());
        when(spotifyTrackMetadataProvider.getTrack("track-1")).thenReturn(metadata);
        when(catalogTrackReadService.getBySpotifyId("track-1")).thenReturn(imported);

        ImportedTrack result = trackImportService.importTrack("track-1");

        assertThat(result).isSameAs(imported);
        InOrder order = inOrder(spotifyTrackMetadataProvider, catalogWriteService, catalogTrackReadService);
        order.verify(spotifyTrackMetadataProvider).getTrack("track-1");
        order.verify(catalogWriteService).upsert(metadata);
        order.verify(catalogTrackReadService).getBySpotifyId("track-1");
    }

    @Test
    void unique_충돌_후_Track이_보이면_기존_결과를_반환한다() {
        SpotifyTrackMetadata metadata = metadata();
        ImportedTrack concurrent = importedTrack();
        when(catalogTrackReadService.findBySpotifyId("track-1"))
                .thenReturn(Optional.empty(), Optional.of(concurrent));
        when(spotifyTrackMetadataProvider.getTrack("track-1")).thenReturn(metadata);
        org.mockito.Mockito.doThrow(new DataIntegrityViolationException("duplicate"))
                .when(catalogWriteService).upsert(metadata);

        ImportedTrack result = trackImportService.importTrack("track-1");

        assertThat(result).isSameAs(concurrent);
        verify(catalogTrackReadService, never()).getBySpotifyId("track-1");
    }

    @Test
    void 부모_row_충돌이면_write를_한번만_재시도한다() {
        SpotifyTrackMetadata metadata = metadata();
        ImportedTrack imported = importedTrack();
        when(catalogTrackReadService.findBySpotifyId("track-1"))
                .thenReturn(Optional.empty(), Optional.empty());
        when(spotifyTrackMetadataProvider.getTrack("track-1")).thenReturn(metadata);
        org.mockito.Mockito.doThrow(new DataIntegrityViolationException("artist duplicate"))
                .doNothing()
                .when(catalogWriteService).upsert(metadata);
        when(catalogTrackReadService.getBySpotifyId("track-1")).thenReturn(imported);

        assertThat(trackImportService.importTrack("track-1")).isSameAs(imported);
        verify(catalogWriteService, org.mockito.Mockito.times(2)).upsert(metadata);
    }

    @Test
    void 두번째_write도_실패하면_예외를_전파한다() {
        SpotifyTrackMetadata metadata = metadata();
        when(catalogTrackReadService.findBySpotifyId("track-1"))
                .thenReturn(Optional.empty(), Optional.empty());
        when(spotifyTrackMetadataProvider.getTrack("track-1")).thenReturn(metadata);
        org.mockito.Mockito.doThrow(new DataIntegrityViolationException("duplicate"))
                .when(catalogWriteService).upsert(metadata);

        assertThatThrownBy(() -> trackImportService.importTrack("track-1"))
                .isInstanceOf(DataIntegrityViolationException.class);
        verify(catalogWriteService, org.mockito.Mockito.times(2)).upsert(metadata);
    }

    private SpotifyTrackMetadata metadata() {
        return SpotifyTrackMetadata.of(
                "track-1", "title", "ISRC", 1000, List.of(),
                "album-1", "album", 2024, List.of()
        );
    }

    private ImportedTrack importedTrack() {
        return ImportedTrack.of(
                1L, "track-1", "title", "ISRC", 1000, List.of(),
                ImportedAlbum.of(2L, "album-1", "album", 2024, List.of())
        );
    }
}
