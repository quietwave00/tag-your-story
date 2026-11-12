package com.tagnote.infrastructure.external.lastfm;

import com.fasterxml.jackson.databind.JsonNode;
import com.tagnote.application.enrichment.config.ExternalEnrichmentProperties;
import com.tagnote.application.enrichment.exception.ExternalProviderException;
import com.tagnote.application.enrichment.matching.model.LastFmCatalogData.TopTags;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Manual, read-only Last.fm comparison. No Spring context, Catalog, or database is used. */
@EnabledIfEnvironmentVariable(named = "LASTFM_DIAGNOSTIC_RUN", matches = "true")
class LastFmLiveDiagnosticTest {
    private static final String BASE_URL = "https://ws.audioscrobbler.com/2.0";

    @Test
    void 알려진_트랙의_원본_태그와_클라이언트_매핑을_비교한다() {
        Comparison result = compare("Cher", "Believe");
        assertThat(result.rawCount())
                .as("The reference track should have Last.fm top tags; check the live API response")
                .isPositive();
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "LASTFM_DIAGNOSTIC_ARTIST", matches = ".+")
    void 요청한_표본의_원본_태그와_클라이언트_매핑을_비교한다() {
        String artist = System.getenv("LASTFM_DIAGNOSTIC_ARTIST");
        String track = System.getenv("LASTFM_DIAGNOSTIC_TRACK");
        assertThat(track).as("LASTFM_DIAGNOSTIC_TRACK must be set with the artist").isNotBlank();
        compare(artist, track);
    }

    private Comparison compare(String artist, String track) {
        String apiKey = System.getenv("LASTFM_API_KEY");
        assertThat(apiKey).as("LASTFM_API_KEY must be set").isNotBlank();

        RestClient http = httpClient();
        JsonNode raw;
        try {
            raw = http.get().uri(uri -> uri.queryParam("method", "track.getTopTags")
                            .queryParam("api_key", apiKey)
                            .queryParam("format", "json")
                            .queryParam("autocorrect", 0)
                            .queryParam("artist", artist)
                            .queryParam("track", track)
                            .build())
                    .retrieve().body(JsonNode.class);
        } catch (RestClientResponseException failure) {
            throw new AssertionError("Last.fm raw request returned HTTP " + failure.getStatusCode().value());
        } catch (RestClientException failure) {
            throw new AssertionError("Last.fm raw request failed: " + failure.getClass().getSimpleName());
        }
        assertThat(raw).as("Last.fm must return a JSON body").isNotNull();
        assertThat(raw.path("error").isMissingNode())
                .as("Last.fm API error code: %s", raw.path("error").asText())
                .isTrue();

        JsonNode topTags = raw.path("toptags");
        JsonNode tags = topTags.path("tag");
        int rawCount = tags.isArray() ? tags.size() : tags.isObject() ? 1 : 0;
        List<String> rawNames = new ArrayList<>();
        if (tags.isArray()) {
            for (int index = 0; index < Math.min(rawCount, 5); index++) {
                rawNames.add(tags.get(index).path("name").asText());
            }
        } else if (tags.isObject()) {
            rawNames.add(tags.path("name").asText());
        }
        System.out.printf("Last.fm raw track artist=%s, title=%s, rawTags=%d, "
                        + "responseArtist=%s, responseTitle=%s, firstTags=%s%n",
                artist, track, rawCount, topTags.path("@attr").path("artist").asText(),
                topTags.path("@attr").path("track").asText(), rawNames);
        ExternalEnrichmentProperties properties = new ExternalEnrichmentProperties();
        properties.getLastfm().setApiKey(apiKey);
        TopTags mapped;
        try {
            mapped = new LastFmClient(http, properties).getTrackTopTags(artist, track);
        } catch (ExternalProviderException failure) {
            throw new AssertionError("Last.fm client mapping/request failed with status " + failure.getStatus());
        }

        System.out.printf("Last.fm track artist=%s, title=%s, rawTags=%d, mappedTags=%d, "
                        + "responseArtist=%s, responseTitle=%s, firstTags=%s%n",
                artist, track, rawCount, mapped.tags().size(), mapped.artistName(), mapped.title(),
                mapped.tags().stream().limit(5).map(tag -> tag.name()).toList());
        assertThat(mapped.tags()).as("Client mapping must preserve the raw tag count").hasSize(rawCount);
        return new Comparison(rawCount, mapped.tags().size());
    }

    private RestClient httpClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(2000);
        factory.setReadTimeout(5000);
        return RestClient.builder().baseUrl(BASE_URL).requestFactory(factory).build();
    }

    private record Comparison(int rawCount, int mappedCount) {}
}
