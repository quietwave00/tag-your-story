package com.tagnote.infrastructure.external;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Manual, live API lookup. Prints raw responses without provider matching or tag resolution.
 * Set EXTERNAL_CATALOG_LOOKUP_RUN=true and the provider credentials to run these live lookups.
 */
@EnabledIfEnvironmentVariable(named = "EXTERNAL_CATALOG_LOOKUP_RUN", matches = "true")
class ExternalCatalogLookupManualTest {

    // For a quick manual run, enter the two search terms here.
    private static final String TRACK = "Symphony";
    private static final String ARTIST = "Lamp";

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    @Test
    void musicBrainzRecordingSearch() throws Exception {
        String track = searchTerm("TRACK", TRACK);
        String artist = searchTerm("ARTIST", ARTIST);
        String userAgent = required("MUSICBRAINZ_USER_AGENT");
        String query = "recording:\"" + escapeMusicBrainz(track) + "\" AND artist:\""
                + escapeMusicBrainz(artist) + "\"";

        lookup("MusicBrainz recording", "https://musicbrainz.org/ws/2/recording?query="
                        + encode(query) + "&limit=10&fmt=json", userAgent, null);
    }

    @Test
    void discogsTrackSearch() throws Exception {
        String track = searchTerm("TRACK", TRACK);
        String artist = searchTerm("ARTIST", ARTIST);
        String token = required("DISCOGS_TOKEN");
        String userAgent = required("DISCOGS_USER_AGENT");

        for (String type : new String[]{"master", "release"}) {
            lookup("Discogs " + type, "https://api.discogs.com/database/search?type=" + type
                            + "&track=" + encode(track) + "&artist=" + encode(artist) + "&per_page=10",
                    userAgent, "Discogs token=" + token);
        }
    }

    @Test
    void lastFmTrackTopTags() throws Exception {
        String track = searchTerm("TRACK", TRACK);
        String artist = searchTerm("ARTIST", ARTIST);
        String apiKey = required("LASTFM_API_KEY");

        lookup("Last.fm track.getTopTags", "https://ws.audioscrobbler.com/2.0/"
                        + "?method=track.getTopTags&artist=" + encode(artist)
                        + "&track=" + encode(track) + "&autocorrect=0&api_key=" + encode(apiKey)
                        + "&format=json", null, null);
    }

    private static String searchTerm(String name, String value) {
        assertTrue(!value.isBlank(), () -> "Set " + name + " at the top of this test");
        return value;
    }

    private static String required(String name) {
        String value = System.getenv(name);
        assertTrue(value != null && !value.isBlank(), () -> "Set " + name + " to run this live lookup");
        return value;
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static String escapeMusicBrainz(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static void lookup(String label, String url, String userAgent, String authorization)
            throws IOException, InterruptedException {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(15))
                .GET();
        if (userAgent != null) {
            request.header("User-Agent", userAgent);
        }
        if (authorization != null) {
            request.header("Authorization", authorization);
        }

        HttpResponse<String> response = HTTP.send(request.build(), HttpResponse.BodyHandlers.ofString());
        System.out.printf("%n=== %s (HTTP %d) ===%n%s%n", label, response.statusCode(), response.body());
        assertEquals(200, response.statusCode(), label + " returned an HTTP error; see response above");
    }
}
