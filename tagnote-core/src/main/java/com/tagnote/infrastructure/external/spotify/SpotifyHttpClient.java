package com.tagnote.infrastructure.external.spotify;

import com.tagnote.core.common.CacheSpec;
import com.tagnote.core.common.CommonRedisTemplate;
import com.tagnote.core.exception.CustomException;
import com.tagnote.core.exception.ExceptionCode;
import com.tagnote.infrastructure.external.spotify.dto.SpotifyJson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.time.Duration;

@Component
public class SpotifyHttpClient {
    private static final int PAGE_SIZE = 10;
    private static final int MAX_RATE_RETRIES = 2;
    private static final long MAX_RETRY_AFTER_SECONDS = 2;

    private final CommonRedisTemplate cache;
    private final RestClient accounts;
    private final RestClient api;
    private final String clientId;
    private final String clientSecret;
    private final Object tokenLock = new Object();

    @Autowired
    public SpotifyHttpClient(CommonRedisTemplate cache,
                             @Value("${spotify.client-id}") String clientId,
                             @Value("${spotify.client-secret}") String clientSecret,
                             @Value("${spotify.accounts-url:https://accounts.spotify.com}") String accountsUrl,
                             @Value("${spotify.api-url:https://api.spotify.com}") String apiUrl,
                             @Value("${spotify.connect-timeout-ms:2000}") int connectTimeoutMs,
                             @Value("${spotify.read-timeout-ms:5000}") int readTimeoutMs) {
        this(cache, clientId, clientSecret,
                restClient(accountsUrl, connectTimeoutMs, readTimeoutMs),
                restClient(apiUrl, connectTimeoutMs, readTimeoutMs));
    }

    SpotifyHttpClient(CommonRedisTemplate cache, String clientId, String clientSecret,
                      RestClient accounts, RestClient api) {
        this.cache = cache;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.accounts = accounts;
        this.api = api;
    }

    private static RestClient restClient(String url, int connectTimeoutMs, int readTimeoutMs) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeoutMs);
        factory.setReadTimeout(readTimeoutMs);
        return RestClient.builder().baseUrl(url).requestFactory(factory).build();
    }

    public SpotifyJson.Page<SpotifyJson.Track> searchTracks(String keyword, int page) {
        SpotifyJson.Search result = search(keyword, page, "track");
        return result.getTracks();
    }

    public SpotifyJson.Search search(String keyword, int page, String types) {
        return get("/v1/search", keyword, page, types, SpotifyJson.Search.class);
    }

    private <T> T get(String path, String keyword, int page, String types, Class<T> type) {
        return request(token -> api.get().uri(builder -> builder.path(path)
                        .queryParam("q", keyword).queryParam("type", types)
                        .queryParam("limit", PAGE_SIZE).queryParam("offset", page * PAGE_SIZE).build())
                .headers(headers -> headers.setBearerAuth(token)).retrieve().body(type));
    }

    public SpotifyJson.Track track(String id) {
        return request(token -> api.get().uri(builder -> builder.path("/v1/tracks/{id}").build(id))
                .headers(headers -> headers.setBearerAuth(token)).retrieve().body(SpotifyJson.Track.class));
    }

    public SpotifyJson.Album album(String id) {
        return request(token -> api.get().uri(builder -> builder.path("/v1/albums/{id}").build(id))
                .headers(headers -> headers.setBearerAuth(token)).retrieve().body(SpotifyJson.Album.class));
    }

    public SpotifyJson.Artist artist(String id) {
        return request(token -> api.get().uri(builder -> builder.path("/v1/artists/{id}").build(id))
                .headers(headers -> headers.setBearerAuth(token)).retrieve().body(SpotifyJson.Artist.class));
    }

    public SpotifyJson.Page<SpotifyJson.Album> artistAlbums(String id, int page) {
        return request(token -> api.get().uri(builder -> builder.path("/v1/artists/{id}/albums")
                        .queryParam("limit", PAGE_SIZE).queryParam("offset", page * PAGE_SIZE).build(id))
                .headers(headers -> headers.setBearerAuth(token)).retrieve()
                .body(new org.springframework.core.ParameterizedTypeReference<SpotifyJson.Page<SpotifyJson.Album>>() {}));
    }

    public SpotifyJson.Page<SpotifyJson.Track> albumTracks(String id, int page) {
        return request(token -> api.get().uri(builder -> builder.path("/v1/albums/{id}/tracks")
                        .queryParam("limit", PAGE_SIZE).queryParam("offset", page * PAGE_SIZE).build(id))
                .headers(headers -> headers.setBearerAuth(token)).retrieve()
                .body(new org.springframework.core.ParameterizedTypeReference<SpotifyJson.Page<SpotifyJson.Track>>() {}));
    }

    private <T> T request(SpotifyRequest<T> action) {
        String token = accessToken();
        boolean refreshed = false;
        int rateRetries = 0;
        while (true) {
            try {
                T response = action.execute(token);
                if (response == null) throw new CustomException(ExceptionCode.SPOTIFY_EXCEPTION);
                return response;
            } catch (RestClientResponseException failure) {
                if (failure.getStatusCode().value() == 401 && !refreshed) {
                    token = refreshAfterUnauthorized(token);
                    refreshed = true;
                    continue;
                }
                if (failure.getStatusCode().value() == 429 && rateRetries++ < MAX_RATE_RETRIES) {
                    if (waitForRetryAfter(failure)) continue;
                }
                throw new CustomException(ExceptionCode.SPOTIFY_EXCEPTION);
            } catch (RestClientException failure) {
                throw new CustomException(ExceptionCode.SPOTIFY_EXCEPTION);
            }
        }
    }

    private String accessToken() {
        String cached = cache.get("", CacheSpec.SPOTIFY_ACCESS_TOKEN);
        if (cached != null && !cached.isBlank()) return cached;
        synchronized (tokenLock) {
            cached = cache.get("", CacheSpec.SPOTIFY_ACCESS_TOKEN);
            return cached != null && !cached.isBlank() ? cached : issueToken();
        }
    }

    private String refreshAfterUnauthorized(String usedToken) {
        synchronized (tokenLock) {
            String cached = cache.get("", CacheSpec.SPOTIFY_ACCESS_TOKEN);
            if (cached != null && !cached.equals(usedToken) && !cached.isBlank()) return cached;
            cache.delete("", CacheSpec.SPOTIFY_ACCESS_TOKEN);
            return issueToken();
        }
    }

    private String issueToken() {
        int rateRetries = 0;
        while (true) {
            try {
                SpotifyJson.Token issued = accounts.post().uri("/api/token")
                        .headers(headers -> headers.setBasicAuth(clientId, clientSecret))
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .body("grant_type=client_credentials")
                        .retrieve().body(SpotifyJson.Token.class);
                if (issued == null || issued.getAccessToken() == null || issued.getAccessToken().isBlank()
                        || issued.getExpiresIn() == null || issued.getExpiresIn() <= 60) {
                    throw new CustomException(ExceptionCode.SPOTIFY_EXCEPTION);
                }
                Duration ttl = Duration.ofSeconds(Math.min(1800, issued.getExpiresIn() - 60L));
                cache.set("", issued.getAccessToken(), CacheSpec.SPOTIFY_ACCESS_TOKEN, ttl);
                return issued.getAccessToken();
            } catch (RestClientResponseException failure) {
                if (failure.getStatusCode().value() == 429 && rateRetries++ < MAX_RATE_RETRIES
                        && waitForRetryAfter(failure)) continue;
                throw new CustomException(ExceptionCode.SPOTIFY_EXCEPTION);
            } catch (RestClientException failure) {
                throw new CustomException(ExceptionCode.SPOTIFY_EXCEPTION);
            }
        }
    }

    private boolean waitForRetryAfter(RestClientResponseException failure) {
        String value = failure.getResponseHeaders() == null ? null
                : failure.getResponseHeaders().getFirst("Retry-After");
        long seconds;
        try { seconds = Long.parseLong(value); }
        catch (NumberFormatException ignored) { return false; }
        if (seconds < 0 || seconds > MAX_RETRY_AFTER_SECONDS) return false;
        try { Thread.sleep(seconds * 1000); }
        catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new CustomException(ExceptionCode.SPOTIFY_EXCEPTION);
        }
        return true;
    }

    @FunctionalInterface
    private interface SpotifyRequest<T> { T execute(String token); }
}
