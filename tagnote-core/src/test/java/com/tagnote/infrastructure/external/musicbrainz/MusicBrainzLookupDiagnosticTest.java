package com.tagnote.infrastructure.external.musicbrainz;

import com.tagnote.application.catalog.importer.model.ImportedAlbum;
import com.tagnote.application.catalog.importer.model.ImportedArtist;
import com.tagnote.application.catalog.importer.model.ImportedTrack;
import com.tagnote.application.catalog.importer.model.SpotifyArtistMetadata;
import com.tagnote.application.catalog.importer.model.SpotifyTrackMetadata;
import com.tagnote.application.enrichment.config.ExternalEnrichmentProperties;
import com.tagnote.application.enrichment.exception.ExternalProviderException;
import com.tagnote.application.enrichment.matching.MusicBrainzEntityMatchingService;
import com.tagnote.application.enrichment.matching.MusicEntityNameNormalizer;
import com.tagnote.application.enrichment.matching.model.MusicBrainzCatalogData.RecordingCandidate;
import com.tagnote.core.common.CommonRedisTemplate;
import com.tagnote.infrastructure.external.spotify.SpotifyHttpClient;
import com.tagnote.infrastructure.external.spotify.SpotifyTrackMetadataAdapter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/** Manual, read-only lookup comparison. Requires no Spring context or database. */
@EnabledIfEnvironmentVariable(named = "MUSICBRAINZ_DIAGNOSTIC_RUN", matches = "true")
class MusicBrainzLookupDiagnosticTest {
    private static final String MUSICBRAINZ_URL = "https://musicbrainz.org/ws/2";
    private static final MusicBrainzRequestGate REQUEST_GATE = requestGate();

    @Test
    void 알려진_ISRC의_조회_경로를_확인한다() {
        ImportedTrack track = ImportedTrack.of(0L, "reference", null, "Creep", "GBAYE9200070", 238640,
                List.of(ImportedArtist.of(0L, "reference-artist", "Radiohead", 0)),
                ImportedAlbum.of(0L, "reference-album", "Pablo Honey", 1993, List.of()));
        Outcome reference = diagnose(track);
        assertThat(reference.status()).as("The known ISRC should be reachable through MusicBrainz")
                .isEqualTo("SUCCESS");
        assertThat(reference.candidates()).as("The known ISRC should return Recording candidates")
                .isNotEmpty();
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "MUSICBRAINZ_DIAGNOSTIC_SPOTIFY_TRACK_ID", matches = ".+")
    void Spotify_표본의_ISRC와_metadata_검색을_각각_확인한다() {
        String clientId = requireEnvironment("SPOTIFY_CLIENT_ID");
        String clientSecret = requireEnvironment("SPOTIFY_CLIENT_SECRET");
        SpotifyHttpClient spotify = new SpotifyHttpClient(mock(CommonRedisTemplate.class),
                clientId, clientSecret, "https://accounts.spotify.com", "https://api.spotify.com", 2000, 5000);
        SpotifyTrackMetadata metadata = new SpotifyTrackMetadataAdapter(spotify)
                .getTrack(requireEnvironment("MUSICBRAINZ_DIAGNOSTIC_SPOTIFY_TRACK_ID"));
        ImportedTrack track = ImportedTrack.of(0L, metadata.getSpotifyTrackId(), null, metadata.getTitle(),
                metadata.getIsrc(), metadata.getDurationMs(), artists(metadata.getTrackArtists()),
                ImportedAlbum.of(0L, metadata.getSpotifyAlbumId(), metadata.getAlbumTitle(),
                        metadata.getReleaseYear(), artists(metadata.getAlbumArtists())));
        diagnose(track);
    }

    private Outcome diagnose(ImportedTrack track) {
        String userAgent = requireEnvironment("MUSICBRAINZ_USER_AGENT");
        ExternalEnrichmentProperties properties = new ExternalEnrichmentProperties();
        properties.getMusicbrainz().setMinimumRequestIntervalMs(1100);
        RestClient http = RestClient.builder().baseUrl(MUSICBRAINZ_URL)
                .requestFactory(requestFactory()).defaultHeader("User-Agent", userAgent).build();
        MusicBrainzClient client = new MusicBrainzClient(http, REQUEST_GATE);
        MusicBrainzEntityMatchingService matching = new MusicBrainzEntityMatchingService(
                new MusicEntityNameNormalizer(), properties);

        String isrc = normalizeIsrc(track.getIsrc());
        System.out.printf("MusicBrainz input spotifyTrackId=%s, title=%s, artists=%s, isrc=%s, durationMs=%s%n",
                track.getSpotifyTrackId(), track.getTitle(),
                track.getArtists().stream().map(ImportedArtist::getName).toList(),
                isrc == null ? "NONE" : isrc, track.getDurationMs());
        Outcome isrcOutcome = null;
        if (isrc != null) {
            isrcOutcome = attempt("ISRC", () -> client.searchByIsrc(isrc));
            print(isrcOutcome, isrcOutcome.candidates() == null ? Optional.empty()
                    : matching.matchRecordingByIsrc(track, isrcOutcome.candidates()));
        }
        Outcome metadataOutcome = attempt("METADATA", () -> client.searchByTitleAndArtists(
                track.getTitle(), track.getArtists().stream().map(ImportedArtist::getName).toList()));
        print(metadataOutcome, metadataOutcome.candidates() == null ? Optional.empty()
                : matching.matchRecordingByMetadata(track, metadataOutcome.candidates()));
        return isrcOutcome;
    }

    private Outcome attempt(String stage, Supplier<List<RecordingCandidate>> request) {
        try {
            return new Outcome(stage, "SUCCESS", null, request.get());
        } catch (ExternalProviderException failure) {
            Integer httpStatus = null;
            Throwable cause = failure.getCause();
            while (cause != null) {
                if (cause instanceof RestClientResponseException response) {
                    httpStatus = response.getStatusCode().value();
                    break;
                }
                cause = cause.getCause();
            }
            return new Outcome(stage, failure.getStatus().name(), httpStatus, null);
        }
    }

    private void print(Outcome outcome, Optional<RecordingCandidate> matched) {
        List<RecordingCandidate> candidates = outcome.candidates();
        System.out.printf("MusicBrainz stage=%s, status=%s, httpStatus=%s, candidateCount=%s, matchedId=%s%n",
                outcome.stage(), outcome.status(), outcome.httpStatus() == null ? "N/A" : outcome.httpStatus(),
                candidates == null ? "N/A" : candidates.size(), matched.map(RecordingCandidate::id).orElse("NONE"));
        if (candidates != null) candidates.stream().limit(5).forEach(candidate ->
                System.out.printf("  candidate id=%s, title=%s, artists=%s, durationMs=%s, score=%s%n",
                        candidate.id(), candidate.title(), candidate.artistNames(), candidate.durationMs(),
                        candidate.score()));
    }

    private String normalizeIsrc(String value) {
        if (value == null || value.isBlank()) return null;
        String normalized = value.replaceAll("[^A-Za-z0-9]", "").toUpperCase(Locale.ROOT);
        return normalized.isBlank() ? null : normalized;
    }

    private List<ImportedArtist> artists(List<SpotifyArtistMetadata> values) {
        return values.stream().map(artist -> ImportedArtist.of(0L, artist.getSpotifyArtistId(),
                artist.getName(), artist.getPosition())).toList();
    }

    private String requireEnvironment(String name) {
        String value = System.getenv(name);
        assertThat(value).as(name + " must be set").isNotBlank();
        return value;
    }

    private SimpleClientHttpRequestFactory requestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(2000);
        factory.setReadTimeout(5000);
        return factory;
    }

    private static MusicBrainzRequestGate requestGate() {
        ExternalEnrichmentProperties properties = new ExternalEnrichmentProperties();
        properties.getMusicbrainz().setMinimumRequestIntervalMs(1100);
        return new MusicBrainzRequestGate(properties);
    }

    private record Outcome(String stage, String status, Integer httpStatus, List<RecordingCandidate> candidates) {}
}
