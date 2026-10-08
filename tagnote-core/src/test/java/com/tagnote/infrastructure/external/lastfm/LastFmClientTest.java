package com.tagnote.infrastructure.external.lastfm;

import com.tagnote.application.enrichment.config.ExternalEnrichmentProperties;
import com.tagnote.application.enrichment.exception.ExternalProviderException;
import com.tagnote.application.enrichment.model.ProviderEnrichmentStatus;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class LastFmClientTest {

    @Test
    void track은_artist_title로_조회하고_mbid를_전달하지_않는다() {
        Fixture fixture = fixture();
        fixture.server.expect(requestTo(allOf(
                        containsString("method=track.getTopTags"),
                        containsString("api_key=fixture-key"),
                        containsString("autocorrect=0"),
                        containsString("artist=Artist"),
                        containsString("track=Track"),
                        not(containsString("mbid="))
                )))
                .andRespond(withSuccess("""
                        {"toptags":{"@attr":{"artist":"Artist","track":"Track"},
                          "tag":[{"name":"Ambient","count":"20"},{"name":"Drone","count":19}]}}
                        """, MediaType.APPLICATION_JSON));

        var result = fixture.client.getTrackTopTags("Artist", "Track");

        assertThat(result.artistName()).isEqualTo("Artist");
        assertThat(result.title()).isEqualTo("Track");
        assertThat(result.tags()).extracting(tag -> tag.count()).containsExactly(20, 19);
        fixture.server.verify();
    }

    @Test
    void provider_rate_limit_error_29는_failed로_분류한다() {
        Fixture fixture = fixture();
        fixture.server.expect(requestTo(containsString("method=album.getTopTags")))
                .andRespond(withSuccess("{\"error\":29,\"message\":\"Rate limit exceeded\"}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> fixture.client.getAlbumTopTags("Artist", "Album"))
                .isInstanceOfSatisfying(ExternalProviderException.class, failure ->
                        assertThat(failure.getStatus()).isEqualTo(ProviderEnrichmentStatus.FAILED)
                );
        fixture.server.verify();
    }

    private Fixture fixture() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://fixture.test/2.0");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        ExternalEnrichmentProperties properties = new ExternalEnrichmentProperties();
        properties.getLastfm().setApiKey("fixture-key");
        return new Fixture(new LastFmClient(builder.build(), properties), server);
    }

    private record Fixture(LastFmClient client, MockRestServiceServer server) {
    }
}
