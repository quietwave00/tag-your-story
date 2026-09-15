package com.tagnote.infrastructure.external.discogs;

import com.tagnote.application.enrichment.config.ExternalEnrichmentProperties;
import com.tagnote.application.enrichment.matching.model.DiscogsCatalogData.EntityType;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class DiscogsClientTest {

    @Test
    void token을_전달하고_search와_master_detail_fixture를_매핑한다() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://fixture.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        ExternalEnrichmentProperties properties = new ExternalEnrichmentProperties();
        properties.getDiscogs().setToken("fixture-token");
        DiscogsClient client = new DiscogsClient(builder.build(), properties);

        server.expect(requestTo(containsString("type=master")))
                .andExpect(requestTo(not(containsString("year="))))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Discogs token=fixture-token"))
                .andRespond(withSuccess("""
                        {"results":[{"id":10,"type":"master","title":"Artist - Album","year":2026}]}
                        """, MediaType.APPLICATION_JSON));
        server.expect(requestTo(containsString("type=release")))
                .andRespond(withSuccess("{\"results\":[]}", MediaType.APPLICATION_JSON));
        server.expect(requestTo(containsString("/masters/10")))
                .andRespond(withSuccess("""
                        {"id":10,"title":"Album","year":2026,"artists":[{"name":"Artist"}],
                         "genres":["Electronic"],"styles":["Ambient"],"ignored":true}
                        """, MediaType.APPLICATION_JSON));

        var masterCandidates = client.searchAlbums("Album", List.of("Artist"), EntityType.MASTER);
        var releaseCandidates = client.searchAlbums("Album", List.of("Artist"), EntityType.RELEASE);
        var details = client.getAlbum(EntityType.MASTER, 10L);

        assertThat(masterCandidates).singleElement().satisfies(candidate -> {
            assertThat(candidate.type()).isEqualTo(EntityType.MASTER);
            assertThat(candidate.title()).isEqualTo("Album");
            assertThat(candidate.artistNames()).containsExactly("Artist");
        });
        assertThat(releaseCandidates).isEmpty();
        assertThat(details.genres()).containsExactly("Electronic");
        assertThat(details.styles()).containsExactly("Ambient");
        server.verify();
    }
}
