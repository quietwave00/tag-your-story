package com.tagnote.application.enrichment.provider;

import com.tagnote.application.catalog.importer.model.ImportedAlbum;
import com.tagnote.application.catalog.importer.model.ImportedArtist;
import com.tagnote.application.catalog.importer.model.ImportedTrack;
import com.tagnote.application.enrichment.config.ExternalEnrichmentProperties;
import com.tagnote.application.enrichment.matching.LastFmEntityMatchingService;
import com.tagnote.application.enrichment.matching.MusicEditionTitleNormalizer;
import com.tagnote.application.enrichment.matching.MusicEntityNameNormalizer;
import com.tagnote.application.enrichment.matching.model.LastFmCatalogData.Tag;
import com.tagnote.application.enrichment.matching.model.LastFmCatalogData.TopTags;
import com.tagnote.application.enrichment.model.EnrichmentDeadline;
import com.tagnote.application.enrichment.model.ProviderEnrichmentStatus;
import com.tagnote.application.enrichment.port.LastFmCatalogClient;
import com.tagnote.domain.enrichment.assertion.EvidenceType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LastFmExternalTagProviderTest {

    @Test
    void count와_무관하게_응답_순서의_상위_5개를_community_tag로_매핑한다() {
        LastFmCatalogClient client = mock(LastFmCatalogClient.class);
        MusicEntityNameNormalizer normalizer = new MusicEntityNameNormalizer();
        ExternalEnrichmentProperties properties = new ExternalEnrichmentProperties();
        LastFmExternalTagProvider provider = new LastFmExternalTagProvider(
                client,
                new LastFmEntityMatchingService(normalizer),
                new MusicEditionTitleNormalizer(normalizer),
                normalizer,
                properties
        );
        ImportedTrack track = track();
        when(client.getTrackTopTags("Track Artist", "Track")).thenReturn(
                new TopTags("Track Artist", "Track", List.of(
                        new Tag("Ambient", 100),
                        new Tag("ambient", 80),
                        new Tag("Drone", 20),
                        new Tag("Too Low", 19),
                        new Tag("Very Low", 1),
                        new Tag("Zero", 0),
                        new Tag("Ignored By Limit", 90)
                ))
        );
        when(client.getAlbumTopTags("Album Artist", "Album")).thenReturn(
                new TopTags("Album Artist", "Album", List.of(new Tag("Electronic", 50)))
        );

        var result = provider.collect(track, deadline());
        var repeated = provider.collect(track, deadline());

        assertThat(repeated.tags()).isEqualTo(result.tags());
        assertThat(result.tags().trackInputs())
                .extracting(input -> input.rawName())
                .containsExactly("Ambient", "Drone", "Too Low", "Very Low", "Zero");
        assertThat(result.tags().trackInputs())
                .allSatisfy(input -> {
                    assertThat(input.evidenceType()).isEqualTo(EvidenceType.COMMUNITY_TAG);
                    assertThat(input.confidence()).isEqualTo(0.65);
                    assertThat(input.externalRef()).startsWith("lastfm:track:").hasSize(77);
                });
        assertThat(result.tags().albumInputs()).singleElement().satisfies(input -> {
            assertThat(input.rawName()).isEqualTo("Electronic");
            assertThat(input.confidence()).isEqualTo(0.60);
            assertThat(input.externalRef()).startsWith("lastfm:album:").hasSize(77);
        });
    }

    @Test
    void track_identity가_불일치해도_exact_album_결과는_보존한다() {
        LastFmCatalogClient client = mock(LastFmCatalogClient.class);
        MusicEntityNameNormalizer normalizer = new MusicEntityNameNormalizer();
        ExternalEnrichmentProperties properties = new ExternalEnrichmentProperties();
        LastFmExternalTagProvider provider = new LastFmExternalTagProvider(
                client,
                new LastFmEntityMatchingService(normalizer),
                new MusicEditionTitleNormalizer(normalizer),
                normalizer,
                properties
        );
        ImportedTrack track = track();
        when(client.getTrackTopTags("Track Artist", "Track")).thenReturn(
                new TopTags("Corrected Artist", "Track", List.of(new Tag("Rock", 100)))
        );
        when(client.getAlbumTopTags("Album Artist", "Album")).thenReturn(
                new TopTags("Album Artist", "Album", List.of(new Tag("Electronic", 50)))
        );

        var result = provider.collect(track, deadline());

        assertThat(result.tags().trackInputs()).isEmpty();
        assertThat(result.tags().albumInputs()).singleElement()
                .extracting(input -> input.rawName())
                .isEqualTo("Electronic");
    }

    @Test
    void 태그가_5개_미만이면_count가_낮아도_반환된_태그만_수집한다() {
        LastFmCatalogClient client = mock(LastFmCatalogClient.class);
        MusicEntityNameNormalizer normalizer = new MusicEntityNameNormalizer();
        ExternalEnrichmentProperties properties = new ExternalEnrichmentProperties();
        LastFmExternalTagProvider provider = new LastFmExternalTagProvider(
                client,
                new LastFmEntityMatchingService(normalizer),
                new MusicEditionTitleNormalizer(normalizer),
                normalizer,
                properties
        );
        ImportedTrack track = track();
        when(client.getTrackTopTags("Track Artist", "Track")).thenReturn(
                new TopTags("Track Artist", "Track", List.of(new Tag("Too Low", 19)))
        );
        when(client.getAlbumTopTags("Album Artist", "Album")).thenReturn(
                new TopTags("Wrong Artist", "Album", List.of(new Tag("Electronic", 50)))
        );

        var result = provider.collect(track, deadline());

        assertThat(result.status()).isEqualTo(ProviderEnrichmentStatus.SUCCESS);
        assertThat(result.tags().trackInputs()).singleElement()
                .extracting(input -> input.rawName())
                .isEqualTo("Too Low");
        assertThat(result.tags().albumInputs()).isEmpty();
    }

    @Test
    void exact_track의_tag가_비어_있으면_edition을_제거한_title로_한번_fallback한다() {
        LastFmCatalogClient client = mock(LastFmCatalogClient.class);
        MusicEntityNameNormalizer normalizer = new MusicEntityNameNormalizer();
        LastFmExternalTagProvider provider = new LastFmExternalTagProvider(
                client,
                new LastFmEntityMatchingService(normalizer),
                new MusicEditionTitleNormalizer(normalizer),
                normalizer,
                new ExternalEnrichmentProperties()
        );
        ImportedTrack track = track("Bohemian Rhapsody - Remastered 2011");
        when(client.getTrackTopTags("Track Artist", "Bohemian Rhapsody - Remastered 2011")).thenReturn(
                new TopTags("Track Artist", "Bohemian Rhapsody - Remastered 2011", List.of())
        );
        when(client.getTrackTopTags("Track Artist", "Bohemian Rhapsody")).thenReturn(
                new TopTags("Track Artist", "Bohemian Rhapsody", List.of(new Tag("Rock", 100)))
        );
        when(client.getAlbumTopTags("Album Artist", "Album")).thenReturn(
                new TopTags("Album Artist", "Album", List.of())
        );

        var result = provider.collect(track, deadline());

        assertThat(result.tags().trackInputs()).singleElement()
                .extracting(input -> input.rawName())
                .isEqualTo("Rock");
        verify(client).getTrackTopTags("Track Artist", "Bohemian Rhapsody - Remastered 2011");
        verify(client).getTrackTopTags("Track Artist", "Bohemian Rhapsody");
    }

    @Test
    void exact_edition_track에_tag가_있으면_canonical_title로_fallback하지_않는다() {
        LastFmCatalogClient client = mock(LastFmCatalogClient.class);
        MusicEntityNameNormalizer normalizer = new MusicEntityNameNormalizer();
        LastFmExternalTagProvider provider = new LastFmExternalTagProvider(
                client,
                new LastFmEntityMatchingService(normalizer),
                new MusicEditionTitleNormalizer(normalizer),
                normalizer,
                new ExternalEnrichmentProperties()
        );
        ImportedTrack track = track("Bohemian Rhapsody - Remastered 2011");
        when(client.getTrackTopTags("Track Artist", "Bohemian Rhapsody - Remastered 2011")).thenReturn(
                new TopTags(
                        "Track Artist",
                        "Bohemian Rhapsody - Remastered 2011",
                        List.of(new Tag("Rock", 100))
                )
        );
        when(client.getAlbumTopTags("Album Artist", "Album")).thenReturn(
                new TopTags("Album Artist", "Album", List.of())
        );

        var result = provider.collect(track, deadline());

        assertThat(result.tags().trackInputs()).singleElement()
                .extracting(input -> input.rawName())
                .isEqualTo("Rock");
        verify(client, never()).getTrackTopTags("Track Artist", "Bohemian Rhapsody");
    }

    private ImportedTrack track() {
        return track("Track");
    }

    private ImportedTrack track(String title) {
        return ImportedTrack.of(
                1L,
                "spotify-track",
                "recording-id",
                title,
                "ISRC",
                180_000,
                List.of(ImportedArtist.of(1L, "track-artist", "Track Artist", 0)),
                ImportedAlbum.of(
                        2L,
                        "spotify-album",
                        "release-group-id",
                        "Album",
                        2026,
                        List.of(ImportedArtist.of(2L, "album-artist", "Album Artist", 0))
                )
        );
    }

    private EnrichmentDeadline deadline() {
        return EnrichmentDeadline.afterMillis(60_000);
    }
}
