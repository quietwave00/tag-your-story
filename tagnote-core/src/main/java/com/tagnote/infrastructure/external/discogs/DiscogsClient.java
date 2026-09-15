package com.tagnote.infrastructure.external.discogs;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.tagnote.application.enrichment.config.ExternalEnrichmentProperties;
import com.tagnote.application.enrichment.matching.model.DiscogsCatalogData.AlbumCandidate;
import com.tagnote.application.enrichment.matching.model.DiscogsCatalogData.AlbumDetails;
import com.tagnote.application.enrichment.matching.model.DiscogsCatalogData.EntityType;
import com.tagnote.application.enrichment.port.DiscogsCatalogClient;
import com.tagnote.infrastructure.external.enrichment.ExternalHttpFailureTranslator;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Locale;

@Component
@ConditionalOnProperty(prefix = "tag.enrichment.discogs", name = "enabled", havingValue = "true")
public class DiscogsClient implements DiscogsCatalogClient {

    private final RestClient restClient;
    private final String authorization;

    public DiscogsClient(
            @Qualifier("discogsRestClient") RestClient restClient,
            ExternalEnrichmentProperties properties
    ) {
        this.restClient = restClient;
        this.authorization = "Discogs token=" + properties.getDiscogs().getToken();
    }

    @Override
    public List<AlbumCandidate> searchAlbums(
            String albumTitle,
            List<String> artists,
            EntityType type
    ) {
        return search(albumTitle, artists, type);
    }

    @Override
    public AlbumDetails getAlbum(EntityType type, long id) {
        String path = type == EntityType.MASTER ? "/masters/{id}" : "/releases/{id}";
        DetailResponse response;
        try {
            response = restClient.get()
                    .uri(path, id)
                    .header(HttpHeaders.AUTHORIZATION, authorization)
                    .retrieve()
                    .body(DetailResponse.class);
        } catch (RestClientException failure) {
            throw ExternalHttpFailureTranslator.translate("Discogs", failure);
        }
        if (response == null) {
            return new AlbumDetails(id, type, null, null, List.of(), List.of(), List.of());
        }
        return new AlbumDetails(
                response.id(),
                type,
                response.title(),
                response.year(),
                safe(response.artists()).stream().map(ArtistDto::name).toList(),
                safe(response.genres()),
                safe(response.styles())
        );
    }

    private List<AlbumCandidate> search(
            String albumTitle,
            List<String> artists,
            EntityType type
    ) {
        SearchResponse response;
        try {
            response = restClient.get()
                    .uri(uri -> {
                        var builder = uri.path("/database/search")
                                .queryParam("type", type.name().toLowerCase(Locale.ROOT))
                                .queryParam("release_title", albumTitle)
                                .queryParam("per_page", 100);
                        if (!artists.isEmpty()) {
                            builder.queryParam("artist", artists.get(0));
                        }
                        return builder.build();
                    })
                    .header(HttpHeaders.AUTHORIZATION, authorization)
                    .retrieve()
                    .body(SearchResponse.class);
        } catch (RestClientException failure) {
            throw ExternalHttpFailureTranslator.translate("Discogs", failure);
        }
        return response == null ? List.of() : safe(response.results()).stream()
                .filter(result -> result.id() > 0)
                .map(result -> toCandidate(result, type))
                .toList();
    }

    private AlbumCandidate toCandidate(SearchResult result, EntityType requestedType) {
        String combined = result.title();
        int separator = combined == null ? -1 : combined.indexOf(" - ");
        String artist = separator < 0 ? null : combined.substring(0, separator);
        String title = separator < 0 ? combined : combined.substring(separator + 3);
        EntityType actualType = parseType(result.type(), requestedType);
        return new AlbumCandidate(
                result.id(),
                actualType,
                title,
                result.year(),
                artist == null ? List.of() : List.of(artist)
        );
    }

    private EntityType parseType(String value, EntityType fallback) {
        if (value == null) {
            return fallback;
        }
        try {
            return EntityType.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return fallback;
        }
    }

    private <T> List<T> safe(List<T> values) {
        return values == null ? List.of() : values;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record SearchResponse(List<SearchResult> results) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record SearchResult(long id, String type, String title, Integer year) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record DetailResponse(
            long id,
            String title,
            Integer year,
            List<ArtistDto> artists,
            List<String> genres,
            List<String> styles
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ArtistDto(String name) {
    }
}
