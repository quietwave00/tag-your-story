package com.tagnote.infrastructure.external.lastfm;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.tagnote.application.enrichment.config.ExternalEnrichmentProperties;
import com.tagnote.application.enrichment.exception.ExternalProviderException;
import com.tagnote.application.enrichment.matching.model.LastFmCatalogData.Tag;
import com.tagnote.application.enrichment.matching.model.LastFmCatalogData.TopTags;
import com.tagnote.application.enrichment.model.ProviderEnrichmentStatus;
import com.tagnote.application.enrichment.port.LastFmCatalogClient;
import com.tagnote.infrastructure.external.enrichment.ExternalHttpFailureTranslator;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;

@Component
@ConditionalOnProperty(prefix = "tag.enrichment.lastfm", name = "enabled", havingValue = "true")
public class LastFmClient implements LastFmCatalogClient {

    private final RestClient restClient;
    private final String apiKey;

    public LastFmClient(
            @Qualifier("lastFmRestClient") RestClient restClient,
            ExternalEnrichmentProperties properties
    ) {
        this.restClient = restClient;
        this.apiKey = properties.getLastfm().getApiKey();
    }

    @Override
    public TopTags getTrackTopTags(String artist, String track) {
        return getTopTags("track.getTopTags", artist, track, "track");
    }

    @Override
    public TopTags getAlbumTopTags(String artist, String album) {
        return getTopTags("album.getTopTags", artist, album, "album");
    }

    private TopTags getTopTags(
            String method,
            String artist,
            String title,
            String titleParameter
    ) {
        Response response;
        try {
            response = restClient.get()
                    .uri(uri -> {
                        var builder = uri.queryParam("method", method)
                                .queryParam("api_key", apiKey)
                                .queryParam("format", "json")
                                .queryParam("autocorrect", 0);
                        builder.queryParam("artist", artist).queryParam(titleParameter, title);
                        return builder.build();
                    })
                    .retrieve()
                    .body(Response.class);
        } catch (RestClientException failure) {
            throw ExternalHttpFailureTranslator.translate("Last.fm", failure);
        }
        if (response == null) {
            throw new ExternalProviderException(ProviderEnrichmentStatus.FAILED, "Last.fm returned no body");
        }
        if (response.error() != null) {
            ProviderEnrichmentStatus status = response.error() == 6 || response.error() == 7
                    ? ProviderEnrichmentStatus.NOT_FOUND
                    : ProviderEnrichmentStatus.FAILED;
            throw new ExternalProviderException(status, "Last.fm API error " + response.error());
        }
        if (response.topTags() == null || response.topTags().attributes() == null) {
            throw new ExternalProviderException(
                    ProviderEnrichmentStatus.NOT_FOUND,
                    "Last.fm top-tag identity was missing"
            );
        }
        Attributes attributes = response.topTags().attributes();
        String responseTitle = attributes.track() == null ? attributes.album() : attributes.track();
        return new TopTags(
                attributes.artist(),
                responseTitle,
                safe(response.topTags().tags()).stream()
                        .map(tag -> new Tag(tag.name(), parseCount(tag.count())))
                        .toList()
        );
    }

    private int parseCount(JsonNode count) {
        if (count == null || count.isNull()) {
            return -1;
        }
        try {
            return Integer.parseInt(count.asText());
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

    private <T> List<T> safe(List<T> values) {
        return values == null ? List.of() : values;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Response(
            Integer error,
            String message,
            @JsonProperty("toptags") TopTagsDto topTags
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record TopTagsDto(
            @JsonProperty("@attr") Attributes attributes,
            @JsonProperty("tag") List<TagDto> tags
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Attributes(String artist, String track, String album) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record TagDto(String name, JsonNode count) {
    }
}
