package com.tagnote.infrastructure.external.musicbrainz;

import com.tagnote.application.enrichment.config.ExternalEnrichmentProperties;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class MusicBrainzClientTest {

    @Test
    void recording_search_lookup과_release_group_genre_fixture를_매핑한다() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://fixture.test/ws/2");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        ExternalEnrichmentProperties properties = new ExternalEnrichmentProperties();
        properties.getMusicbrainz().setMinimumRequestIntervalMs(1);
        MusicBrainzRequestGate gate = new MusicBrainzRequestGate(properties);
        MusicBrainzClient client = new MusicBrainzClient(builder.build(), gate);

        server.expect(requestTo(containsString("/isrc/USABC1234567?")))
                .andExpect(requestTo(containsString("artist-credits")))
                .andExpect(request -> assertThat(request.getURI().toString()).doesNotContain("release-groups"))
                .andExpect(request -> assertThat(request.getURI().toString()).doesNotContain("genres"))
                .andExpect(request -> assertThat(request.getURI().toString()).doesNotContain("releases"))
                .andRespond(withSuccess("""
                {"recordings":[{"id":"recording-1","title":"Track","length":180000,
                  "artist-credit":[{"name":"Artist"}],"ignored":"value"}]}
                """, MediaType.APPLICATION_JSON));
        server.expect(requestTo(containsString("/recording/recording-1?")))
                .andExpect(requestTo(containsString("genres")))
                .andExpect(request -> assertThat(request.getURI().toString()).doesNotContain("releases"))
                .andRespond(withSuccess("""
                {"id":"recording-1","genres":[{"name":"Ambient","count":4}]}
                """, MediaType.APPLICATION_JSON));
        server.expect(requestTo(containsString("/recording/recording-1?")))
                .andExpect(requestTo(containsString("releases")))
                .andExpect(request -> assertThat(request.getURI().toString()).doesNotContain("genres"))
                .andRespond(withSuccess("""
                {"id":"recording-1",
                 "releases":[{"date":"2026-01-01","artist-credit":[{"name":"Artist"}],
                   "release-group":{"id":"release-group-1","title":"Album",
                     "first-release-date":"2026-01-01"}}]}
                """, MediaType.APPLICATION_JSON));
        server.expect(requestTo(containsString("/release-group/release-group-1?"))).andRespond(withSuccess("""
                {"id":"release-group-1","genres":[{"name":"Electronic","count":2}]}
                """, MediaType.APPLICATION_JSON));

        var candidates = client.searchByIsrc("USABC1234567");
        var recording = client.getRecording("recording-1");
        var recordingReleaseGroups = client.getRecordingReleaseGroups("recording-1");
        var releaseGroup = client.getReleaseGroup("release-group-1");

        assertThat(candidates).singleElement().satisfies(candidate -> {
            assertThat(candidate.id()).isEqualTo("recording-1");
            assertThat(candidate.artistNames()).containsExactly("Artist");
        });
        assertThat(recording.genres()).singleElement().satisfies(genre -> {
            assertThat(genre.name()).isEqualTo("Ambient");
            assertThat(genre.count()).isEqualTo(4);
        });
        assertThat(recording.releaseGroups()).isEmpty();
        assertThat(recordingReleaseGroups.releaseGroups()).singleElement().satisfies(group -> {
            assertThat(group.id()).isEqualTo("release-group-1");
            assertThat(group.releaseYear()).isEqualTo(2026);
        });
        assertThat(releaseGroup.genres()).extracting(genre -> genre.name()).containsExactly("Electronic");
        server.verify();
    }
}
