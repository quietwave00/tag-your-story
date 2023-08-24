package com.tagstory.core.domain.tracks.util;

import com.mysql.cj.util.StringUtils;
import com.tagstory.core.config.CacheSpec;
import com.tagstory.core.domain.user.redis.TagStoryRedisTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.exceptions.SpotifyWebApiException;
import se.michaelthelin.spotify.model_objects.credentials.ClientCredentials;
import se.michaelthelin.spotify.requests.authorization.client_credentials.ClientCredentialsRequest;

import java.io.IOException;

@Component
@Slf4j
@RequiredArgsConstructor
public class SpotifyWebClient {
//    @Value("${spotify.client-id}")
    private final String CLIENT_ID = "c4b7b1bc3705473984707f49762a01bc";
//    @Value("${spotify.client-secret}")
    private final String CLIENT_SECRET = "913a13e08d71401286973a288516c4a5";
    private final SpotifyApi spotifyApi = new SpotifyApi.Builder().setClientId(CLIENT_ID).setClientSecret(CLIENT_SECRET).build();

    private final TagStoryRedisTemplate redisTemplate;

    public String generateAccessToken() {
        ClientCredentialsRequest clientCredentialsRequest = spotifyApi.clientCredentials().build();
        try {
            final ClientCredentials clientCredentials = clientCredentialsRequest.execute();
            spotifyApi.setAccessToken(clientCredentials.getAccessToken());
            return spotifyApi.getAccessToken();
        } catch (IOException | SpotifyWebApiException | org.apache.hc.core5.http.ParseException e) {
            log.error(e.getMessage());
        }
        return null;
    }

    public SpotifyApi getSpotifyApi() {
        return new SpotifyApi.Builder()
                .setAccessToken(getAccessToken())
                .build();
    }

    @Cacheable(value = "spotifyAccessToken")
    public String getAccessToken() {
        String accessToken = redisTemplate.get("", CacheSpec.SPOTIFY_ACCESS_TOKEN);
        if(StringUtils.isNullOrEmpty(accessToken)) {
            accessToken = generateAccessToken();
            redisTemplate.set("", accessToken, CacheSpec.SPOTIFY_ACCESS_TOKEN);
        }
        return accessToken;
    }
}
