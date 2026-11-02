package com.tagnote.infrastructure.external.spotify;

import com.tagnote.core.common.CacheSpec;
import com.tagnote.core.common.CommonRedisTemplate;
import com.tagnote.core.exception.CustomException;
import com.tagnote.core.exception.ExceptionCode;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class SpotifyHttpClientTest {
    private final CommonRedisTemplate cache = mock(CommonRedisTemplate.class);
    private final RestClient.Builder accountsBuilder = RestClient.builder().baseUrl("https://accounts.spotify.test");
    private final RestClient.Builder apiBuilder = RestClient.builder().baseUrl("https://api.spotify.test");
    private final MockRestServiceServer accounts = MockRestServiceServer.bindTo(accountsBuilder).build();
    private final MockRestServiceServer api = MockRestServiceServer.bindTo(apiBuilder).build();
    private final SpotifyHttpClient client = new SpotifyHttpClient(cache, "client", "secret",
            accountsBuilder.build(), apiBuilder.build());

    @Test
    void token_uses_basic_credentials_existing_cache_key_and_bounded_ttl() {
        accounts.expect(once(), requestTo("https://accounts.spotify.test/api/token"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Basic Y2xpZW50OnNlY3JldA=="))
                .andExpect(content().string("grant_type=client_credentials"))
                .andRespond(withSuccess("{\"access_token\":\"token\",\"expires_in\":3600}", MediaType.APPLICATION_JSON));
        api.expect(once(), request -> assertThat(request.getURI().toString())
                        .contains("/v1/search?").contains("q=rock").contains("type=track")
                        .contains("limit=10").contains("offset=20"))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer token"))
                .andRespond(withSuccess("{\"tracks\":{\"total\":1,\"items\":[]}}", MediaType.APPLICATION_JSON));
        assertThat(client.searchTracks("rock", 2).getTotal()).isEqualTo(1);
        verify(cache).set("", "token", CacheSpec.SPOTIFY_ACCESS_TOKEN, Duration.ofMinutes(30));
        accounts.verify();
        api.verify();
    }

    @Test
    void 아티스트_앨범_후속_페이지에_정확한_offset을_보낸다() {
        when(cache.get("", CacheSpec.SPOTIFY_ACCESS_TOKEN)).thenReturn("token");
        api.expect(once(), request -> assertThat(request.getURI().toString())
                        .contains("/v1/artists/ar/albums?").contains("limit=10").contains("offset=20"))
                .andRespond(withSuccess("{\"total\":31,\"items\":[{\"id\":\"album-21\",\"album_type\":\"album\",\"total_tracks\":6}]}",
                        MediaType.APPLICATION_JSON));

        assertThat(client.artistAlbums("ar", 2).getItems()[0].getId()).isEqualTo("album-21");
        api.verify();
    }

    @Test
    void unauthorized_invalidates_cached_token_and_retries_once() {
        AtomicReference<String> cached = new AtomicReference<>("old");
        when(cache.get("", CacheSpec.SPOTIFY_ACCESS_TOKEN)).thenAnswer(call -> cached.get());
        doAnswer(call -> { cached.set(null); return true; }).when(cache).delete("", CacheSpec.SPOTIFY_ACCESS_TOKEN);
        doAnswer(call -> { cached.set(call.getArgument(1)); return null; })
                .when(cache).set(eq(""), anyString(), eq(CacheSpec.SPOTIFY_ACCESS_TOKEN), any(Duration.class));
        api.expect(once(), requestTo("https://api.spotify.test/v1/tracks/t1"))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer old"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));
        accounts.expect(once(), requestTo("https://accounts.spotify.test/api/token"))
                .andRespond(withSuccess("{\"access_token\":\"new\",\"expires_in\":120}", MediaType.APPLICATION_JSON));
        api.expect(once(), requestTo("https://api.spotify.test/v1/tracks/t1"))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer new"))
                .andRespond(withSuccess("{\"id\":\"t1\"}", MediaType.APPLICATION_JSON));
        assertThat(client.track("t1").getId()).isEqualTo("t1");
        verify(cache).set("", "new", CacheSpec.SPOTIFY_ACCESS_TOKEN, Duration.ofSeconds(60));
        api.verify();
        accounts.verify();
    }

    @Test
    void rate_limit_with_zero_retry_after_retries_then_returns_failure() {
        when(cache.get("", CacheSpec.SPOTIFY_ACCESS_TOKEN)).thenReturn("token");
        for (int i = 0; i < 3; i++) {
            api.expect(once(), requestTo("https://api.spotify.test/v1/artists/ar"))
                    .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS).header("Retry-After", "0"));
        }
        assertThatThrownBy(() -> client.artist("ar")).isInstanceOf(CustomException.class)
                .satisfies(failure -> assertThat(((CustomException) failure).getExceptionCode())
                        .isEqualTo(ExceptionCode.SPOTIFY_EXCEPTION));
        api.verify();
    }
}
