package com.tagnote.application.enrichment.provider;

import com.tagnote.application.catalog.importer.model.ImportedAlbum;
import com.tagnote.application.catalog.importer.model.ImportedArtist;
import com.tagnote.application.catalog.importer.model.ImportedTrack;
import com.tagnote.application.enrichment.config.ExternalEnrichmentProperties;
import com.tagnote.application.enrichment.exception.ExternalProviderException;
import com.tagnote.application.enrichment.matching.MusicBrainzEntityMatchingService;
import com.tagnote.application.enrichment.matching.MusicEntityNameNormalizer;
import com.tagnote.application.enrichment.matching.MusicEditionTitleNormalizer;
import com.tagnote.application.enrichment.matching.model.MusicBrainzCatalogData.Genre;
import com.tagnote.application.enrichment.matching.model.MusicBrainzCatalogData.RecordingCandidate;
import com.tagnote.application.enrichment.matching.model.MusicBrainzCatalogData.RecordingDetails;
import com.tagnote.application.enrichment.matching.model.MusicBrainzCatalogData.ReleaseGroupCandidate;
import com.tagnote.application.enrichment.matching.model.MusicBrainzCatalogData.ReleaseGroupDetails;
import com.tagnote.application.enrichment.model.EnrichmentDeadline;
import com.tagnote.application.enrichment.model.ProviderEnrichmentStatus;
import com.tagnote.application.enrichment.port.MusicBrainzCatalogClient;
import com.tagnote.domain.enrichment.assertion.EvidenceType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MusicBrainzExternalTagProviderTest {

    @Test
    void remastered_album은_canonical_title을_첫_요청에_사용한다() {
        MusicBrainzCatalogClient client = mock(MusicBrainzCatalogClient.class);
        ExternalEnrichmentProperties properties = new ExternalEnrichmentProperties();
        MusicEntityNameNormalizer normalizer = new MusicEntityNameNormalizer();
        MusicBrainzExternalTagProvider provider = new MusicBrainzExternalTagProvider(
                client, new MusicBrainzEntityMatchingService(normalizer, properties),
                normalizer, new MusicEditionTitleNormalizer(normalizer), properties);
        ImportedAlbum album = ImportedAlbum.of(2L, "spotify-album", "Nevermind (Remastered)", 1991,
                List.of(ImportedArtist.of(1L, "spotify-artist", "Nirvana", 0)));
        ReleaseGroupCandidate group = new ReleaseGroupCandidate("rg-nevermind", "Nevermind", 1991,
                List.of("Nirvana"));
        when(client.searchReleaseGroups("Nevermind", List.of("Nirvana")))
                .thenReturn(List.of(group));
        when(client.getReleaseGroup("rg-nevermind"))
                .thenReturn(new ReleaseGroupDetails("rg-nevermind", List.of(new Genre("Grunge", 2))));

        var result = provider.collectAlbum(album, deadline());

        verify(client).searchReleaseGroups("Nevermind", List.of("Nirvana"));
        verify(client, never()).searchReleaseGroups("Nevermind (Remastered)", List.of("Nirvana"));
        assertThat(result.tags().albumInputs()).singleElement()
                .extracting(input -> input.rawName()).isEqualTo("Grunge");
    }

    @Test
    void canonical_title에_일치_후보가_없으면_원문_title로_한번_재검색한다() {
        MusicBrainzCatalogClient client = mock(MusicBrainzCatalogClient.class);
        ExternalEnrichmentProperties properties = new ExternalEnrichmentProperties();
        MusicEntityNameNormalizer normalizer = new MusicEntityNameNormalizer();
        MusicBrainzExternalTagProvider provider = new MusicBrainzExternalTagProvider(
                client, new MusicBrainzEntityMatchingService(normalizer, properties),
                normalizer, new MusicEditionTitleNormalizer(normalizer), properties);
        ImportedAlbum album = ImportedAlbum.of(2L, "spotify-album", "Nevermind (Remastered)", 2011,
                List.of(ImportedArtist.of(1L, "spotify-artist", "Nirvana", 0)));
        when(client.searchReleaseGroups("Nevermind (Remastered)", List.of("Nirvana")))
                .thenReturn(List.of(new ReleaseGroupCandidate("rg-remaster",
                        "Nevermind (Remastered)", 2011, List.of("Nirvana"))));
        when(client.getReleaseGroup("rg-remaster"))
                .thenReturn(new ReleaseGroupDetails("rg-remaster", List.of(new Genre("Rock", 1))));

        var result = provider.collectAlbum(album, deadline());

        verify(client).searchReleaseGroups("Nevermind", List.of("Nirvana"));
        verify(client).searchReleaseGroups("Nevermind (Remastered)", List.of("Nirvana"));
        assertThat(result.tags().albumInputs()).singleElement()
                .extracting(input -> input.rawName()).isEqualTo("Rock");
    }

    @Test
    void canonical_title이_맞아도_release_group_연도가_다르면_확정하지_않는다() {
        MusicBrainzCatalogClient client = mock(MusicBrainzCatalogClient.class);
        ExternalEnrichmentProperties properties = new ExternalEnrichmentProperties();
        MusicEntityNameNormalizer normalizer = new MusicEntityNameNormalizer();
        MusicBrainzExternalTagProvider provider = new MusicBrainzExternalTagProvider(
                client, new MusicBrainzEntityMatchingService(normalizer, properties),
                normalizer, new MusicEditionTitleNormalizer(normalizer), properties);
        ImportedAlbum album = ImportedAlbum.of(2L, "spotify-album", "Nevermind (Remastered)", 2011,
                List.of(ImportedArtist.of(1L, "spotify-artist", "Nirvana", 0)));
        when(client.searchReleaseGroups("Nevermind", List.of("Nirvana")))
                .thenReturn(List.of(new ReleaseGroupCandidate("rg-nevermind", "Nevermind", 1991,
                        List.of("Nirvana"))));

        assertThatThrownBy(() -> provider.collectAlbum(album, deadline()))
                .isInstanceOf(ExternalProviderException.class)
                .hasMessageContaining("matchedCount=0");
        verify(client, never()).getReleaseGroup("rg-nevermind");
    }

    @Test
    void canonical_title에_복수_group이_맞으면_원문_fallback을_실행하지_않는다() {
        MusicBrainzCatalogClient client = mock(MusicBrainzCatalogClient.class);
        ExternalEnrichmentProperties properties = new ExternalEnrichmentProperties();
        MusicEntityNameNormalizer normalizer = new MusicEntityNameNormalizer();
        MusicBrainzExternalTagProvider provider = new MusicBrainzExternalTagProvider(
                client, new MusicBrainzEntityMatchingService(normalizer, properties),
                normalizer, new MusicEditionTitleNormalizer(normalizer), properties);
        ImportedAlbum album = ImportedAlbum.of(2L, "spotify-album", "Nevermind (Remastered)", 2011,
                List.of(ImportedArtist.of(1L, "spotify-artist", "Nirvana", 0)));
        when(client.searchReleaseGroups("Nevermind", List.of("Nirvana")))
                .thenReturn(List.of(
                        new ReleaseGroupCandidate("first", "Nevermind", 2011, List.of("Nirvana")),
                        new ReleaseGroupCandidate("second", "Nevermind", 2011, List.of("Nirvana"))));

        assertThatThrownBy(() -> provider.collectAlbum(album, deadline()))
                .isInstanceOf(ExternalProviderException.class)
                .hasMessageContaining("matchedCount=2");
        verify(client, never()).searchReleaseGroups("Nevermind (Remastered)", List.of("Nirvana"));
    }

    @Test
    void 저장된_Recording_Mbid는_search없이_재사용하고_Album_장르도_매핑한다() {
        MusicBrainzCatalogClient client = mock(MusicBrainzCatalogClient.class);
        ExternalEnrichmentProperties properties = new ExternalEnrichmentProperties();
        MusicEntityNameNormalizer normalizer = new MusicEntityNameNormalizer();
        MusicBrainzExternalTagProvider provider = new MusicBrainzExternalTagProvider(
                client,
                new MusicBrainzEntityMatchingService(normalizer, properties),
                normalizer,
                new MusicEditionTitleNormalizer(normalizer),
                properties
        );
        ImportedTrack track = track("recording-1");
        when(client.getRecording("recording-1")).thenReturn(new RecordingDetails(
                "recording-1",
                List.of(new Genre("Ambient", 2), new Genre("Ignored", 0)),
                List.of()
        ));
        when(client.getRecordingReleaseGroups("recording-1")).thenReturn(new RecordingDetails(
                "recording-1", List.of(),
                List.of(new ReleaseGroupCandidate("release-group-1", "Album", 2026, List.of("Artist")))
        ));
        when(client.getReleaseGroup("release-group-1")).thenReturn(new ReleaseGroupDetails(
                "release-group-1", List.of(new Genre("Electronic", 1))
        ));

        var result = provider.collect(track, deadline());

        verify(client, never()).searchByIsrc(org.mockito.ArgumentMatchers.anyString());
        verify(client, never()).searchByTitleAndArtists(
                org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyList()
        );
        assertThat(result.identityMatch().musicBrainzRecordingId()).isEqualTo("recording-1");
        assertThat(result.tags().trackInputs()).singleElement().satisfies(input -> {
            assertThat(input.rawName()).isEqualTo("Ambient");
            assertThat(input.evidenceType()).isEqualTo(EvidenceType.EXPLICIT_GENRE);
            assertThat(input.confidence()).isEqualTo(0.90);
            assertThat(input.externalRef()).isEqualTo("musicbrainz:recording:recording-1");
        });
        assertThat(result.tags().albumInputs()).singleElement().satisfies(input -> {
            assertThat(input.rawName()).isEqualTo("Electronic");
            assertThat(input.confidence()).isEqualTo(0.75);
            assertThat(input.externalRef()).isEqualTo("musicbrainz:release-group:release-group-1");
        });
    }

    @Test
    void release_group_조회실패는_recording_결과와_identity를_폐기하지_않는다() {
        MusicBrainzCatalogClient client = mock(MusicBrainzCatalogClient.class);
        ExternalEnrichmentProperties properties = new ExternalEnrichmentProperties();
        MusicEntityNameNormalizer normalizer = new MusicEntityNameNormalizer();
        MusicBrainzExternalTagProvider provider = new MusicBrainzExternalTagProvider(
                client,
                new MusicBrainzEntityMatchingService(normalizer, properties),
                normalizer,
                new MusicEditionTitleNormalizer(normalizer),
                properties
        );
        ImportedTrack track = track("recording-1");
        when(client.getRecording("recording-1")).thenReturn(new RecordingDetails(
                "recording-1", List.of(new Genre("Ambient", 2)), List.of()
        ));
        when(client.getRecordingReleaseGroups("recording-1")).thenReturn(new RecordingDetails(
                "recording-1", List.of(),
                List.of(new ReleaseGroupCandidate("release-group-1", "Album", 2026, List.of("Artist")))
        ));
        when(client.getReleaseGroup("release-group-1")).thenThrow(new ExternalProviderException(
                ProviderEnrichmentStatus.TIMEOUT, "fixture timeout"
        ));

        var result = provider.collect(track, deadline());

        assertThat(result.status()).isEqualTo(ProviderEnrichmentStatus.SUCCESS);
        assertThat(result.tags().trackInputs()).singleElement()
                .extracting(input -> input.rawName())
                .isEqualTo("Ambient");
        assertThat(result.tags().albumInputs()).isEmpty();
        assertThat(result.identityMatch().musicBrainzRecordingId()).isEqualTo("recording-1");
    }

    @Test
    void 유일한_Isrc_후보는_artist가_일치하면_title과_duration이_달라도_fallback없이_채택한다() {
        MusicBrainzCatalogClient client = mock(MusicBrainzCatalogClient.class);
        ExternalEnrichmentProperties properties = new ExternalEnrichmentProperties();
        MusicEntityNameNormalizer normalizer = new MusicEntityNameNormalizer();
        MusicBrainzExternalTagProvider provider = new MusicBrainzExternalTagProvider(
                client,
                new MusicBrainzEntityMatchingService(normalizer, properties),
                normalizer,
                new MusicEditionTitleNormalizer(normalizer),
                properties
        );
        ImportedTrack track = track(null);
        RecordingCandidate candidate = new RecordingCandidate(
                "recording-original", "Original Title", 240_000, List.of("Artist")
        );
        when(client.searchByIsrc("USABC1234567")).thenReturn(List.of(candidate));
        when(client.getRecording("recording-original")).thenReturn(new RecordingDetails(
                "recording-original", List.of(new Genre("Rock", 1)), List.of()
        ));
        when(client.getRecordingReleaseGroups("recording-original")).thenReturn(new RecordingDetails(
                "recording-original",
                List.of(),
                List.of(new ReleaseGroupCandidate(
                        "release-group-1", "Album", 2026, List.of("Artist")
                ))
        ));
        when(client.getReleaseGroup("release-group-1")).thenReturn(new ReleaseGroupDetails(
                "release-group-1", List.of(new Genre("Classic Rock", 1))
        ));

        var result = provider.collect(track, deadline());

        verify(client, never()).searchByTitleAndArtists(
                org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyList()
        );
        verify(client).getRecording("recording-original");
        assertThat(result.identityMatch().musicBrainzRecordingId()).isEqualTo("recording-original");
        assertThat(result.tags().trackInputs()).singleElement()
                .extracting(input -> input.rawName())
                .isEqualTo("Rock");
        assertThat(result.tags().albumInputs()).singleElement()
                .extracting(input -> input.rawName())
                .isEqualTo("Classic Rock");
    }

    @Test
    void 복수_Isrc_후보는_duration이_가장_가까운_단일후보를_fallback없이_채택한다() {
        MusicBrainzCatalogClient client = mock(MusicBrainzCatalogClient.class);
        ExternalEnrichmentProperties properties = new ExternalEnrichmentProperties();
        MusicEntityNameNormalizer normalizer = new MusicEntityNameNormalizer();
        MusicBrainzExternalTagProvider provider = new MusicBrainzExternalTagProvider(
                client,
                new MusicBrainzEntityMatchingService(normalizer, properties),
                normalizer,
                new MusicEditionTitleNormalizer(normalizer),
                properties
        );
        ImportedTrack track = track(null, "Creep", "GBAYE9200070", 235_640);
        RecordingCandidate closest = new RecordingCandidate(
                "70595637-9310-45f2-a266-58f8de4874a7", "Creep", 236_666, List.of("Artist")
        );
        when(client.searchByIsrc("GBAYE9200070")).thenReturn(List.of(
                closest,
                new RecordingCandidate(
                        "1f9f17c7-6085-4d52-9bd0-13a94cc449bc", "Creep", 237_680, List.of("Artist")
                )
        ));
        when(client.getRecording(closest.id())).thenReturn(new RecordingDetails(
                closest.id(), List.of(new Genre("Alternative Rock", 1)), List.of()
        ));
        when(client.getRecordingReleaseGroups(closest.id())).thenReturn(new RecordingDetails(
                closest.id(), List.of(), List.of()
        ));

        var result = provider.collect(track, deadline());

        verify(client, never()).searchByTitleAndArtists(
                org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyList()
        );
        verify(client).getRecording(closest.id());
        assertThat(result.identityMatch().musicBrainzRecordingId()).isEqualTo(closest.id());
    }

    @Test
    void Isrc_후보가_없으면_title과_artist_search로_fallback한다() {
        MusicBrainzCatalogClient client = mock(MusicBrainzCatalogClient.class);
        ExternalEnrichmentProperties properties = new ExternalEnrichmentProperties();
        MusicEntityNameNormalizer normalizer = new MusicEntityNameNormalizer();
        MusicBrainzExternalTagProvider provider = new MusicBrainzExternalTagProvider(
                client,
                new MusicBrainzEntityMatchingService(normalizer, properties),
                normalizer,
                new MusicEditionTitleNormalizer(normalizer),
                properties
        );
        ImportedTrack track = track(null);
        RecordingCandidate candidate = new RecordingCandidate(
                "recording-2", "Track", 180_000, List.of("Artist")
        );
        when(client.searchByIsrc("USABC1234567")).thenReturn(List.of());
        when(client.searchByTitleAndArtists("Track", List.of("Artist"))).thenReturn(List.of(candidate));
        when(client.getRecording("recording-2")).thenReturn(new RecordingDetails(
                "recording-2", List.of(new Genre("Ambient", 1)), List.of()
        ));
        when(client.getRecordingReleaseGroups("recording-2")).thenReturn(new RecordingDetails(
                "recording-2", List.of(), List.of()
        ));

        var result = provider.collect(track, deadline());

        verify(client).searchByIsrc("USABC1234567");
        verify(client).searchByTitleAndArtists("Track", List.of("Artist"));
        assertThat(result.identityMatch().musicBrainzRecordingId()).isEqualTo("recording-2");
    }

    @Test
    void 첫_필수요청_timeout은_deadline안에서_한번만_재시도한다() {
        MusicBrainzCatalogClient client = mock(MusicBrainzCatalogClient.class);
        ExternalEnrichmentProperties properties = new ExternalEnrichmentProperties();
        MusicEntityNameNormalizer normalizer = new MusicEntityNameNormalizer();
        MusicBrainzExternalTagProvider provider = new MusicBrainzExternalTagProvider(
                client,
                new MusicBrainzEntityMatchingService(normalizer, properties),
                normalizer,
                new MusicEditionTitleNormalizer(normalizer),
                properties
        );
        ImportedTrack track = track(null);
        RecordingCandidate candidate = new RecordingCandidate(
                "recording-original", "Original Title", 240_000, List.of("Artist")
        );
        when(client.searchByIsrc("USABC1234567"))
                .thenThrow(new ExternalProviderException(ProviderEnrichmentStatus.TIMEOUT, "fixture timeout"))
                .thenReturn(List.of(candidate));
        when(client.getRecording("recording-original")).thenReturn(new RecordingDetails(
                "recording-original", List.of(new Genre("Rock", 1)), List.of()
        ));
        when(client.getRecordingReleaseGroups("recording-original")).thenReturn(new RecordingDetails(
                "recording-original", List.of(), List.of()
        ));

        var result = provider.collect(track, deadline());

        verify(client, times(2)).searchByIsrc("USABC1234567");
        verify(client).getRecording("recording-original");
        assertThat(result.tags().trackInputs()).singleElement()
                .extracting(input -> input.rawName())
                .isEqualTo("Rock");
    }

    @Test
    void 첫_필수요청_503은_deadline안에서_한번_재시도한다() {
        MusicBrainzCatalogClient client = mock(MusicBrainzCatalogClient.class);
        ExternalEnrichmentProperties properties = new ExternalEnrichmentProperties();
        MusicEntityNameNormalizer normalizer = new MusicEntityNameNormalizer();
        MusicBrainzExternalTagProvider provider = new MusicBrainzExternalTagProvider(
                client,
                new MusicBrainzEntityMatchingService(normalizer, properties),
                normalizer,
                new MusicEditionTitleNormalizer(normalizer),
                properties
        );
        ImportedTrack track = track(null);
        RecordingCandidate candidate = new RecordingCandidate(
                "recording-original", "Original Title", 240_000, List.of("Artist")
        );
        when(client.searchByIsrc("USABC1234567"))
                .thenThrow(ExternalProviderException.retryable(
                        ProviderEnrichmentStatus.FAILED,
                        "MusicBrainz request failed with HTTP 503"
                ))
                .thenReturn(List.of(candidate));
        when(client.getRecording("recording-original")).thenReturn(new RecordingDetails(
                "recording-original", List.of(new Genre("Rock", 1)), List.of()
        ));
        when(client.getRecordingReleaseGroups("recording-original")).thenReturn(new RecordingDetails(
                "recording-original", List.of(), List.of()
        ));

        var result = provider.collect(track, deadline());

        verify(client, times(2)).searchByIsrc("USABC1234567");
        assertThat(result.tags().trackInputs()).singleElement()
                .extracting(input -> input.rawName())
                .isEqualTo("Rock");
    }

    @Test
    void 필수요청_503이_계속되면_총_2회_요청후_실패한다() {
        MusicBrainzCatalogClient client = mock(MusicBrainzCatalogClient.class);
        ExternalEnrichmentProperties properties = new ExternalEnrichmentProperties();
        MusicEntityNameNormalizer normalizer = new MusicEntityNameNormalizer();
        MusicBrainzExternalTagProvider provider = new MusicBrainzExternalTagProvider(
                client,
                new MusicBrainzEntityMatchingService(normalizer, properties),
                normalizer,
                new MusicEditionTitleNormalizer(normalizer),
                properties
        );
        ExternalProviderException unavailable = ExternalProviderException.retryable(
                ProviderEnrichmentStatus.FAILED,
                "MusicBrainz request failed with HTTP 503"
        );
        when(client.searchByIsrc("USABC1234567")).thenThrow(unavailable);

        assertThatThrownBy(() -> provider.collect(track(null), deadline()))
                .isSameAs(unavailable);
        verify(client, times(2)).searchByIsrc("USABC1234567");
    }

    @Test
    void 잔여_budget이_부족하면_release_group을_생략하고_recording결과를_보존한다() {
        MusicBrainzCatalogClient client = mock(MusicBrainzCatalogClient.class);
        ExternalEnrichmentProperties properties = new ExternalEnrichmentProperties();
        MusicEntityNameNormalizer normalizer = new MusicEntityNameNormalizer();
        MusicBrainzExternalTagProvider provider = new MusicBrainzExternalTagProvider(
                client,
                new MusicBrainzEntityMatchingService(normalizer, properties),
                normalizer,
                new MusicEditionTitleNormalizer(normalizer),
                properties
        );
        ImportedTrack track = track("recording-1");
        when(client.getRecording("recording-1")).thenReturn(new RecordingDetails(
                "recording-1", List.of(new Genre("Ambient", 2)), List.of()
        ));

        var result = provider.collect(track, EnrichmentDeadline.expired());

        verify(client, never()).getReleaseGroup("release-group-1");
        verify(client, never()).getRecordingReleaseGroups("recording-1");
        assertThat(result.status()).isEqualTo(ProviderEnrichmentStatus.SUCCESS);
        assertThat(result.tags().trackInputs()).singleElement()
                .extracting(input -> input.rawName())
                .isEqualTo("Ambient");
        assertThat(result.tags().albumInputs()).isEmpty();
        assertThat(result.identityMatch().musicBrainzRecordingId()).isEqualTo("recording-1");
    }

    @Test
    void 필수단계의_첫_timeout은_요청순서와_무관하게_한번만_재시도한다() {
        MusicBrainzCatalogClient client = mock(MusicBrainzCatalogClient.class);
        ExternalEnrichmentProperties properties = new ExternalEnrichmentProperties();
        MusicEntityNameNormalizer normalizer = new MusicEntityNameNormalizer();
        MusicBrainzExternalTagProvider provider = new MusicBrainzExternalTagProvider(
                client,
                new MusicBrainzEntityMatchingService(normalizer, properties),
                normalizer,
                new MusicEditionTitleNormalizer(normalizer),
                properties
        );
        ImportedTrack track = track(null);
        when(client.searchByIsrc("USABC1234567")).thenReturn(List.of());
        RecordingCandidate candidate = new RecordingCandidate(
                "recording-2", "Track", 180_000, List.of("Artist")
        );
        when(client.searchByTitleAndArtists("Track", List.of("Artist"))).thenReturn(List.of(candidate));
        when(client.getRecording("recording-2"))
                .thenThrow(new ExternalProviderException(ProviderEnrichmentStatus.TIMEOUT, "fixture timeout"))
                .thenReturn(new RecordingDetails(
                        "recording-2", List.of(new Genre("Ambient", 1)), List.of()
                ));
        when(client.getRecordingReleaseGroups("recording-2")).thenReturn(new RecordingDetails(
                "recording-2", List.of(), List.of()
        ));

        var result = provider.collect(track, deadline());

        verify(client).searchByIsrc("USABC1234567");
        verify(client).searchByTitleAndArtists("Track", List.of("Artist"));
        verify(client, times(2)).getRecording("recording-2");
        assertThat(result.tags().trackInputs()).singleElement()
                .extracting(input -> input.rawName())
                .isEqualTo("Ambient");
    }

    @Test
    void optional_recording_releases_timeout은_recording결과를_폐기하지_않는다() {
        MusicBrainzCatalogClient client = mock(MusicBrainzCatalogClient.class);
        ExternalEnrichmentProperties properties = new ExternalEnrichmentProperties();
        MusicEntityNameNormalizer normalizer = new MusicEntityNameNormalizer();
        MusicBrainzExternalTagProvider provider = new MusicBrainzExternalTagProvider(
                client,
                new MusicBrainzEntityMatchingService(normalizer, properties),
                normalizer,
                new MusicEditionTitleNormalizer(normalizer),
                properties
        );
        ImportedTrack track = track("recording-1");
        when(client.getRecording("recording-1")).thenReturn(new RecordingDetails(
                "recording-1", List.of(new Genre("Ambient", 2)), List.of()
        ));
        when(client.getRecordingReleaseGroups("recording-1")).thenThrow(new ExternalProviderException(
                ProviderEnrichmentStatus.TIMEOUT, "fixture timeout"
        ));

        var result = provider.collect(track, deadline());

        assertThat(result.status()).isEqualTo(ProviderEnrichmentStatus.SUCCESS);
        assertThat(result.tags().trackInputs()).singleElement()
                .extracting(input -> input.rawName())
                .isEqualTo("Ambient");
        assertThat(result.tags().albumInputs()).isEmpty();
        assertThat(result.identityMatch().musicBrainzRecordingId()).isEqualTo("recording-1");
    }

    private ImportedTrack track(String recordingId) {
        return track(recordingId, "Track", "US-ABC-12-34567", 180_000);
    }

    private ImportedTrack track(String recordingId, String title, String isrc, int durationMs) {
        return ImportedTrack.of(
                1L,
                "spotify-track",
                recordingId,
                title,
                isrc,
                durationMs,
                List.of(ImportedArtist.of(1L, "artist", "Artist", 0)),
                ImportedAlbum.of(
                        2L,
                        "spotify-album",
                        "Album",
                        2026,
                        List.of(ImportedArtist.of(1L, "artist", "Artist", 0))
                )
        );
    }

    private EnrichmentDeadline deadline() {
        return EnrichmentDeadline.afterMillis(60_000);
    }
}
