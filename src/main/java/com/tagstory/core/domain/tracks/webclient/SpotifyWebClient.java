package com.tagstory.core.domain.tracks.webclient;

import com.mysql.cj.util.StringUtils;
import com.tagstory.api.exception.CustomException;
import com.tagstory.api.exception.ExceptionCode;
import com.tagstory.core.config.CacheSpec;
import com.tagstory.core.domain.user.redis.TagStoryRedisTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.core5.http.ParseException;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.exceptions.SpotifyWebApiException;
import se.michaelthelin.spotify.model_objects.credentials.ClientCredentials;
import se.michaelthelin.spotify.model_objects.specification.Paging;
import se.michaelthelin.spotify.model_objects.specification.Track;
import se.michaelthelin.spotify.requests.authorization.client_credentials.ClientCredentialsRequest;
import se.michaelthelin.spotify.requests.data.search.simplified.SearchTracksRequest;
import se.michaelthelin.spotify.requests.data.tracks.GetTrackRequest;

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

    private String generateAccessToken() {
        ClientCredentialsRequest clientCredentialsRequest = spotifyApi.clientCredentials().build();
        try {
            final ClientCredentials clientCredentials = clientCredentialsRequest.execute();
            spotifyApi.setAccessToken(clientCredentials.getAccessToken());
            return spotifyApi.getAccessToken();
        } catch (IOException | SpotifyWebApiException | org.apache.hc.core5.http.ParseException e) {
            log.error(e.getMessage());
            throw new CustomException(ExceptionCode.SPOTIFY_EXCEPTION);
        }
    }

    private SpotifyApi getSpotifyApi() {
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

    public Track[] getTrackInfoByKeyword(String keyword, int page) {
        try {
            SpotifyApi spotifyApi = getSpotifyApi();
            SearchTracksRequest searchTrackRequest = spotifyApi.searchTracks(keyword)
                    .limit(10)
                    .offset(page)
                    .build();
            Paging<Track> searchResult = searchTrackRequest.execute();
            return searchResult.getItems();
        } catch (IOException | ParseException | SpotifyWebApiException e) {
            log.error(e.getMessage());
            throw new CustomException(ExceptionCode.SPOTIFY_EXCEPTION);
        }
    }

    public Track getDetailTrackInfo(String trackId) {
        try {
            SpotifyApi spotifyApi = getSpotifyApi();
            GetTrackRequest request = spotifyApi.getTrack(trackId).build();
            return request.execute();
        } catch(IOException | ParseException | SpotifyWebApiException e) {
            log.error(e.getMessage());
            throw new CustomException(ExceptionCode.SPOTIFY_EXCEPTION);
        }
    }
}
