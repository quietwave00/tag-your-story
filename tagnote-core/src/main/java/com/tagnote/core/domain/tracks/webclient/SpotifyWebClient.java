package com.tagnote.core.domain.tracks.webclient;

import org.apache.commons.lang3.StringUtils;
import com.tagnote.core.common.CommonRedisTemplate;
import com.tagnote.core.common.CacheSpec;
import com.tagnote.core.domain.tracks.webclient.dto.TrackInfo;
import com.tagnote.core.exception.CustomException;
import com.tagnote.core.exception.ExceptionCode;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.core5.http.ParseException;
import org.springframework.beans.factory.annotation.Value;
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
public class SpotifyWebClient {
    private final CommonRedisTemplate redisTemplate;
    private final SpotifyApi spotifyApi;

    public SpotifyWebClient(
            CommonRedisTemplate redisTemplate,
            @Value("${spotify.client-id}") String clientId,
            @Value("${spotify.client-secret}") String clientSecret
    ) {
        this.redisTemplate = redisTemplate;
        this.spotifyApi = new SpotifyApi.Builder()
                .setClientId(clientId)
                .setClientSecret(clientSecret)
                .build();
    }

    /*
     * 스포티파이 라이브러리의 AccessToken을 생성한다.
     */
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

    /*
     * SpotifyApi를 반환한다.
     */
    private SpotifyApi getSpotifyApi() {
        return new SpotifyApi.Builder()
                .setAccessToken(getAccessToken())
                .build();
    }

    /*
     * 레디스에서 AccessToken을 반환한다.
     * 값이 없을 시 생성하여 반환한다.
     */
    public String getAccessToken() {
        String accessToken = redisTemplate.get("", CacheSpec.SPOTIFY_ACCESS_TOKEN);
        if(StringUtils.isEmpty(accessToken)) {
            accessToken = generateAccessToken();
            redisTemplate.set("", accessToken, CacheSpec.SPOTIFY_ACCESS_TOKEN);
        }
        return accessToken;
    }

    /*
     * 키워드에 따른 검색 결과를 반환한다.
     */
    public TrackInfo getTrackInfoByKeyword(String keyword, int page) {
        try {
            SpotifyApi spotifyApi = getSpotifyApi();
            SearchTracksRequest searchTrackRequest = spotifyApi.searchTracks(keyword)
                    .limit(10)
                    .offset(page * 10)
                    .build();
            Paging<Track> searchResult = searchTrackRequest.execute();
            Track[] tracks = searchResult.getItems();
            return TrackInfo.of(tracks, searchResult.getTotal());
        } catch (IOException | ParseException | SpotifyWebApiException e) {
            log.error(e.getMessage());
            throw new CustomException(ExceptionCode.SPOTIFY_EXCEPTION);
        }
    }

    /*
     * 트랙 아이디에 따른 상세정보를 반환한다.
     */
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
