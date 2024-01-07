package com.tagstory.core.domain.tracks.webclient;

import org.apache.commons.lang3.StringUtils;
import com.tagstory.core.common.CommonRedisTemplate;
import com.tagstory.core.common.CacheSpec;
import com.tagstory.core.domain.tracks.webclient.dto.TrackInfo;
import com.tagstory.core.exception.CustomException;
import com.tagstory.core.exception.ExceptionCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.core5.http.ParseException;
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

    private final CommonRedisTemplate redisTemplate;

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
