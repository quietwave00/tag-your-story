package com.tagstory.core.domain.tracks.util;

import com.wrapper.spotify.SpotifyApi;
import com.wrapper.spotify.exceptions.SpotifyWebApiException;
import com.wrapper.spotify.model_objects.credentials.ClientCredentials;
import com.wrapper.spotify.requests.authorization.client_credentials.ClientCredentialsRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Slf4j
public class SpotifyUtil {
//    @Value("${spotify.client-id}")
    private final String CLIENT_ID = "c4b7b1bc3705473984707f49762a01bc";

//    @Value("${spotify.client-secret}")
    private final String CLIENT_SECRET = "913a13e08d71401286973a288516c4a5";

    private final SpotifyApi spotifyApi = new SpotifyApi.Builder().setClientId(CLIENT_ID).setClientSecret(CLIENT_SECRET).build();

    public String accessToken() {
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
                .setAccessToken(accessToken())
                .build();
    }
}
