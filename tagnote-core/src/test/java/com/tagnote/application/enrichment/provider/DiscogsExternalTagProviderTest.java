package com.tagnote.application.enrichment.provider;

import com.tagnote.application.catalog.importer.model.ImportedAlbum;
import com.tagnote.application.catalog.importer.model.ImportedArtist;
import com.tagnote.application.catalog.importer.model.ImportedTrack;
import com.tagnote.application.enrichment.config.ExternalEnrichmentProperties;
import com.tagnote.application.enrichment.exception.ExternalProviderException;
import com.tagnote.application.enrichment.matching.DiscogsAlbumIdentityNormalizer;
import com.tagnote.application.enrichment.matching.DiscogsAlbumMatchingService;
import com.tagnote.application.enrichment.matching.DiscogsAlbumSearchPlan;
import com.tagnote.application.enrichment.matching.MusicEditionTitleNormalizer;
import com.tagnote.application.enrichment.matching.MusicEntityNameNormalizer;
import com.tagnote.application.enrichment.matching.model.DiscogsCatalogData.AlbumCandidate;
import com.tagnote.application.enrichment.matching.model.DiscogsCatalogData.AlbumDetails;
import com.tagnote.application.enrichment.matching.model.DiscogsCatalogData.AlbumSearchQuery;
import com.tagnote.application.enrichment.matching.model.DiscogsCatalogData.EntityType;
import com.tagnote.application.enrichment.matching.model.DiscogsCatalogData.SearchField;
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
                searchPlan(normalizer),
                normalizer,
                properties
        );
        ImportedTrack track = track();
        AlbumCandidate candidate = new AlbumCandidate(
                10L, EntityType.MASTER, "Album", 2026, List.of("Album Artist")
        );
        when(client.searchAlbums(query(SearchField.RELEASE_TITLE, "Album", "Album Artist"), EntityType.MASTER))
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
                query(SearchField.RELEASE_TITLE, "Album", "Album Artist"), EntityType.RELEASE
        );
        verify(client, never()).searchAlbums(
                query(SearchField.TRACK, "Track", "Track Artist"), EntityType.MASTER
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
                searchPlan(normalizer),
                normalizer,
                properties
        );
        ImportedTrack track = track();
        AlbumCandidate release = new AlbumCandidate(
                20L, EntityType.RELEASE, "Album", 2026, List.of("Album Artist")
        );
        when(client.searchAlbums(query(SearchField.RELEASE_TITLE, "Album", "Album Artist"), EntityType.MASTER))
                .thenReturn(List.of(new AlbumCandidate(
                        10L, EntityType.MASTER, "Different Album", 2026, List.of("Album Artist")
                )));
        when(client.searchAlbums(query(SearchField.RELEASE_TITLE, "Album", "Album Artist"), EntityType.RELEASE))
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
                searchPlan(normalizer),
                normalizer,
                properties
        );
        when(client.searchAlbums(query(SearchField.RELEASE_TITLE, "Album", "Album Artist"), EntityType.MASTER))
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
                query(SearchField.RELEASE_TITLE, "Album", "Album Artist"), EntityType.RELEASE
        );
        verify(client, never()).searchAlbums(
                query(SearchField.TRACK, "Track", "Track Artist"), EntityType.MASTER
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
                searchPlan(normalizer),
                normalizer,
                properties
        );
        when(client.searchAlbums(query(SearchField.RELEASE_TITLE, "Album", "Album Artist"), EntityType.MASTER))
                .thenReturn(List.of());
        when(client.searchAlbums(query(SearchField.RELEASE_TITLE, "Album", "Album Artist"), EntityType.RELEASE))
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
        verify(client, never()).searchAlbums(
                query(SearchField.TRACK, "Track", "Track Artist"), EntityType.MASTER
        );
    }

    @Test
    void Queen_remaster_album은_canonical_title로_master를_조회한다() {
        DiscogsCatalogClient client = mock(DiscogsCatalogClient.class);
        MusicEntityNameNormalizer normalizer = new MusicEntityNameNormalizer();
        ExternalEnrichmentProperties properties = new ExternalEnrichmentProperties();
        DiscogsExternalTagProvider provider = new DiscogsExternalTagProvider(
                client,
                matchingService(normalizer),
                searchPlan(normalizer),
                normalizer,
                properties
        );
        ImportedTrack track = track("A Night At The Opera (2011 Remaster)", 1975, "Queen");
        AlbumCandidate master = new AlbumCandidate(
                5863L, EntityType.MASTER, "A Night At The Opera", 1975, List.of("Queen")
        );
        when(client.searchAlbums(
                query(SearchField.RELEASE_TITLE, "A Night At The Opera", "Queen"),
                EntityType.MASTER
        ))
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
                query(SearchField.RELEASE_TITLE, "A Night At The Opera", "Queen"),
                EntityType.RELEASE
        );
    }

    @Test
    void box_set_원문이_없으면_slash목록을_제거한_album_title로_재검색한다() {
        DiscogsCatalogClient client = mock(DiscogsCatalogClient.class);
        MusicEntityNameNormalizer normalizer = new MusicEntityNameNormalizer();
        DiscogsExternalTagProvider provider = new DiscogsExternalTagProvider(
                client,
                matchingService(normalizer),
                searchPlan(normalizer),
                normalizer,
                new ExternalEnrichmentProperties()
        );
        String spotifyTitle = "5 Album Set (Pablo Honey/The Bends/OK Computer/Kid A/Amnesiac)";
        ImportedTrack track = track(spotifyTitle, 2012, "Radiohead");
        AlbumSearchQuery original = query(SearchField.RELEASE_TITLE, spotifyTitle, "Radiohead");
        AlbumSearchQuery shortened = query(SearchField.RELEASE_TITLE, "5 Album Set", "Radiohead");
        AlbumCandidate release = new AlbumCandidate(
                4_019_733L, EntityType.RELEASE, "5 Album Set", 2012, List.of("Radiohead")
        );
        when(client.searchAlbums(original, EntityType.MASTER)).thenReturn(List.of());
        when(client.searchAlbums(original, EntityType.RELEASE)).thenReturn(List.of());
        when(client.searchAlbums(shortened, EntityType.MASTER)).thenReturn(List.of());
        when(client.searchAlbums(shortened, EntityType.RELEASE)).thenReturn(List.of(release));
        when(client.getAlbum(EntityType.RELEASE, 4_019_733L)).thenReturn(new AlbumDetails(
                4_019_733L,
                EntityType.RELEASE,
                "5 Album Set",
                2012,
                List.of("Radiohead"),
                List.of("Rock"),
                List.of("Alternative Rock")
        ));

        var result = provider.collect(track, deadline());

        assertThat(result.tags().albumInputs())
                .extracting(input -> input.externalRef())
                .containsOnly("discogs:release:4019733");
        verify(client, never()).searchAlbums(
                query(SearchField.TRACK, "Track", "Track Artist"), EntityType.MASTER
        );
    }

    @Test
    void album_title_검색들이_비면_track_title로_유일한_album을_찾는다() {
        DiscogsCatalogClient client = mock(DiscogsCatalogClient.class);
        MusicEntityNameNormalizer normalizer = new MusicEntityNameNormalizer();
        DiscogsExternalTagProvider provider = new DiscogsExternalTagProvider(
                client,
                matchingService(normalizer),
                searchPlan(normalizer),
                normalizer,
                new ExternalEnrichmentProperties()
        );
        ImportedTrack track = track();
        AlbumSearchQuery albumQuery = query(SearchField.RELEASE_TITLE, "Album", "Album Artist");
        AlbumSearchQuery trackQuery = query(SearchField.TRACK, "Track", "Track Artist");
        AlbumCandidate release = new AlbumCandidate(
                20L, EntityType.RELEASE, "Album", 2026, List.of("Album Artist")
        );
        when(client.searchAlbums(albumQuery, EntityType.MASTER)).thenReturn(List.of());
        when(client.searchAlbums(albumQuery, EntityType.RELEASE)).thenReturn(List.of());
        when(client.searchAlbums(trackQuery, EntityType.MASTER)).thenReturn(List.of());
        when(client.searchAlbums(trackQuery, EntityType.RELEASE)).thenReturn(List.of(release));
        when(client.getAlbum(EntityType.RELEASE, 20L)).thenReturn(new AlbumDetails(
                20L,
                EntityType.RELEASE,
                "Album",
                2026,
                List.of("Album Artist"),
                List.of("Rock"),
                List.of()
        ));

        var result = provider.collect(track, deadline());

        assertThat(result.tags().albumInputs()).singleElement()
                .extracting(input -> input.externalRef())
                .isEqualTo("discogs:release:20");
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
                        albumTitle,
                        releaseYear,
                        List.of(ImportedArtist.of(2L, "album-artist", albumArtist, 0))
                )
        );
    }

    private DiscogsAlbumMatchingService matchingService(MusicEntityNameNormalizer normalizer) {
        return new DiscogsAlbumMatchingService(identityNormalizer(normalizer));
    }

    private DiscogsAlbumSearchPlan searchPlan(MusicEntityNameNormalizer normalizer) {
        return new DiscogsAlbumSearchPlan(identityNormalizer(normalizer));
    }

    private DiscogsAlbumIdentityNormalizer identityNormalizer(MusicEntityNameNormalizer normalizer) {
        return new DiscogsAlbumIdentityNormalizer(normalizer, new MusicEditionTitleNormalizer(normalizer));
    }

    private AlbumSearchQuery query(SearchField field, String value, String artist) {
        return new AlbumSearchQuery(field, value, List.of(artist));
    }

    private EnrichmentDeadline deadline() {
        return EnrichmentDeadline.afterMillis(60_000);
    }
}
