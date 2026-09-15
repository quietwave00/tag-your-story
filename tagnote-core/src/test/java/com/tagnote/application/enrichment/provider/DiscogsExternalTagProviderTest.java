package com.tagnote.application.enrichment.provider;

import com.tagnote.application.catalog.importer.model.ImportedAlbum;
import com.tagnote.application.catalog.importer.model.ImportedArtist;
import com.tagnote.application.catalog.importer.model.ImportedTrack;
import com.tagnote.application.enrichment.config.ExternalEnrichmentProperties;
import com.tagnote.application.enrichment.exception.ExternalProviderException;
import com.tagnote.application.enrichment.matching.DiscogsAlbumIdentityNormalizer;
import com.tagnote.application.enrichment.matching.DiscogsAlbumMatchingService;
import com.tagnote.application.enrichment.matching.MusicEditionTitleNormalizer;
import com.tagnote.application.enrichment.matching.MusicEntityNameNormalizer;
import com.tagnote.application.enrichment.matching.model.DiscogsCatalogData.AlbumCandidate;
import com.tagnote.application.enrichment.matching.model.DiscogsCatalogData.AlbumDetails;
import com.tagnote.application.enrichment.matching.model.DiscogsCatalogData.EntityType;
import com.tagnote.application.enrichment.model.EnrichmentDeadline;
import com.tagnote.application.enrichment.port.DiscogsCatalogClient;
import com.tagnote.domain.enrichment.assertion.EvidenceType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DiscogsExternalTagProviderTest {

    @Test
    void 유일하게_검증된_album의_genre와_style을_각_evidence로_매핑한다() {
        DiscogsCatalogClient client = mock(DiscogsCatalogClient.class);
        MusicEntityNameNormalizer normalizer = new MusicEntityNameNormalizer();
        ExternalEnrichmentProperties properties = new ExternalEnrichmentProperties();
        DiscogsExternalTagProvider provider = new DiscogsExternalTagProvider(
                client,
                matchingService(normalizer),
                normalizer,
                properties
        );
        ImportedTrack track = track();
        AlbumCandidate candidate = new AlbumCandidate(
                10L, EntityType.MASTER, "Album", 2026, List.of("Album Artist")
        );
        when(client.searchAlbums("Album", List.of("Album Artist"), EntityType.MASTER))
                .thenReturn(List.of(candidate));
        when(client.getAlbum(EntityType.MASTER, 10L)).thenReturn(new AlbumDetails(
                10L,
                EntityType.MASTER,
                "Album",
                2026,
                List.of("Album Artist"),
                List.of("Electronic"),
                List.of("Ambient")
        ));

        var result = provider.collect(track, deadline());

        assertThat(result.tags().trackInputs()).isEmpty();
        assertThat(result.tags().albumInputs()).hasSize(2);
        assertThat(result.tags().albumInputs())
                .extracting(input -> input.evidenceType())
                .containsExactly(EvidenceType.EXPLICIT_GENRE, EvidenceType.EXPLICIT_STYLE);
        assertThat(result.tags().albumInputs())
                .extracting(input -> input.confidence())
                .containsExactly(0.70, 0.85);
        assertThat(result.tags().albumInputs())
                .extracting(input -> input.externalRef())
                .containsOnly("discogs:master:10");
        verify(client, never()).searchAlbums(
                "Album", List.of("Album Artist"), EntityType.RELEASE
        );
    }

    @Test
    void exact_master가_없으면_유일한_release로_fallback한다() {
        DiscogsCatalogClient client = mock(DiscogsCatalogClient.class);
        MusicEntityNameNormalizer normalizer = new MusicEntityNameNormalizer();
        ExternalEnrichmentProperties properties = new ExternalEnrichmentProperties();
        DiscogsExternalTagProvider provider = new DiscogsExternalTagProvider(
                client,
                matchingService(normalizer),
                normalizer,
                properties
        );
        ImportedTrack track = track();
        AlbumCandidate release = new AlbumCandidate(
                20L, EntityType.RELEASE, "Album", 2026, List.of("Album Artist")
        );
        when(client.searchAlbums("Album", List.of("Album Artist"), EntityType.MASTER))
                .thenReturn(List.of(new AlbumCandidate(
                        10L, EntityType.MASTER, "Different Album", 2026, List.of("Album Artist")
                )));
        when(client.searchAlbums("Album", List.of("Album Artist"), EntityType.RELEASE))
                .thenReturn(List.of(release));
        when(client.getAlbum(EntityType.RELEASE, 20L)).thenReturn(new AlbumDetails(
                20L,
                EntityType.RELEASE,
                "Album",
                2026,
                List.of("Album Artist"),
                List.of("Electronic"),
                List.of("Ambient")
        ));

        var result = provider.collect(track, deadline());

        assertThat(result.tags().albumInputs())
                .extracting(input -> input.externalRef())
                .containsOnly("discogs:release:20");
    }

    @Test
    void exact_master가_복수이면_release로_fallback하지_않는다() {
        DiscogsCatalogClient client = mock(DiscogsCatalogClient.class);
        MusicEntityNameNormalizer normalizer = new MusicEntityNameNormalizer();
        ExternalEnrichmentProperties properties = new ExternalEnrichmentProperties();
        DiscogsExternalTagProvider provider = new DiscogsExternalTagProvider(
                client,
                matchingService(normalizer),
                normalizer,
                properties
        );
        when(client.searchAlbums("Album", List.of("Album Artist"), EntityType.MASTER))
                .thenReturn(List.of(
                        new AlbumCandidate(
                                10L, EntityType.MASTER, "Album", 2026, List.of("Album Artist")
                        ),
                        new AlbumCandidate(
                                11L, EntityType.MASTER, "Album", 2026, List.of("Album Artist")
                        )
                ));

        assertThatThrownBy(() -> provider.collect(track(), deadline()))
                .isInstanceOf(ExternalProviderException.class)
                .hasMessageContaining("masterMatches=2");
        verify(client, never()).searchAlbums(
                "Album", List.of("Album Artist"), EntityType.RELEASE
        );
    }

    @Test
    void exact_release가_복수이면_임의로_선택하지_않는다() {
        DiscogsCatalogClient client = mock(DiscogsCatalogClient.class);
        MusicEntityNameNormalizer normalizer = new MusicEntityNameNormalizer();
        ExternalEnrichmentProperties properties = new ExternalEnrichmentProperties();
        DiscogsExternalTagProvider provider = new DiscogsExternalTagProvider(
                client,
                matchingService(normalizer),
                normalizer,
                properties
        );
        when(client.searchAlbums("Album", List.of("Album Artist"), EntityType.MASTER))
                .thenReturn(List.of());
        when(client.searchAlbums("Album", List.of("Album Artist"), EntityType.RELEASE))
                .thenReturn(List.of(
                        new AlbumCandidate(
                                20L, EntityType.RELEASE, "Album", 2026, List.of("Album Artist")
                        ),
                        new AlbumCandidate(
                                21L, EntityType.RELEASE, "Album", 2026, List.of("Album Artist")
                        )
                ));

        assertThatThrownBy(() -> provider.collect(track(), deadline()))
                .isInstanceOf(ExternalProviderException.class)
                .hasMessageContaining("masterMatches=0, releaseMatches=2");
        verify(client, never()).getAlbum(EntityType.RELEASE, 20L);
        verify(client, never()).getAlbum(EntityType.RELEASE, 21L);
    }

    @Test
    void Queen_remaster_album은_canonical_title로_master를_조회한다() {
        DiscogsCatalogClient client = mock(DiscogsCatalogClient.class);
        MusicEntityNameNormalizer normalizer = new MusicEntityNameNormalizer();
        ExternalEnrichmentProperties properties = new ExternalEnrichmentProperties();
        DiscogsExternalTagProvider provider = new DiscogsExternalTagProvider(
                client,
                matchingService(normalizer),
                normalizer,
                properties
        );
        ImportedTrack track = track("A Night At The Opera (2011 Remaster)", 1975, "Queen");
        AlbumCandidate master = new AlbumCandidate(
                5863L, EntityType.MASTER, "A Night At The Opera", 1975, List.of("Queen")
        );
        when(client.searchAlbums("A Night At The Opera", List.of("Queen"), EntityType.MASTER))
                .thenReturn(List.of(master));
        when(client.getAlbum(EntityType.MASTER, 5863L)).thenReturn(new AlbumDetails(
                5863L,
                EntityType.MASTER,
                "A Night At The Opera",
                1975,
                List.of("Queen"),
                List.of("Rock"),
                List.of("Classic Rock")
        ));

        var result = provider.collect(track, deadline());

        assertThat(result.tags().albumInputs())
                .extracting(input -> input.externalRef())
                .containsOnly("discogs:master:5863");
        verify(client, never()).searchAlbums(
                "A Night At The Opera", List.of("Queen"), EntityType.RELEASE
        );
    }

    private ImportedTrack track() {
        return track("Album", 2026, "Album Artist");
    }

    private ImportedTrack track(String albumTitle, int releaseYear, String albumArtist) {
        return ImportedTrack.of(
                1L,
                "spotify-track",
                null,
                "Track",
                "ISRC",
                180_000,
                List.of(ImportedArtist.of(1L, "artist", "Track Artist", 0)),
                ImportedAlbum.of(
                        2L,
                        "spotify-album",
                        null,
                        albumTitle,
                        releaseYear,
                        List.of(ImportedArtist.of(2L, "album-artist", albumArtist, 0))
                )
        );
    }

    private DiscogsAlbumMatchingService matchingService(MusicEntityNameNormalizer normalizer) {
        return new DiscogsAlbumMatchingService(new DiscogsAlbumIdentityNormalizer(
                normalizer,
                new MusicEditionTitleNormalizer(normalizer)
        ));
    }

    private EnrichmentDeadline deadline() {
        return EnrichmentDeadline.afterMillis(60_000);
    }
}
