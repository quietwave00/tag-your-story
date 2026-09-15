package com.tagnote.infrastructure.external.musicbrainz;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.tagnote.application.enrichment.matching.model.MusicBrainzCatalogData.Genre;
import com.tagnote.application.enrichment.matching.model.MusicBrainzCatalogData.RecordingCandidate;
import com.tagnote.application.enrichment.matching.model.MusicBrainzCatalogData.RecordingDetails;
import com.tagnote.application.enrichment.matching.model.MusicBrainzCatalogData.ReleaseGroupCandidate;
import com.tagnote.application.enrichment.matching.model.MusicBrainzCatalogData.ReleaseGroupDetails;
import com.tagnote.application.enrichment.port.MusicBrainzCatalogClient;
import com.tagnote.infrastructure.external.enrichment.ExternalHttpFailureTranslator;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.LinkedHashMap;
import java.util.List;

@Component
@ConditionalOnProperty(prefix = "tag.enrichment.musicbrainz", name = "enabled", havingValue = "true")
public class MusicBrainzClient implements MusicBrainzCatalogClient {

    private final RestClient restClient;
    private final MusicBrainzRequestGate requestGate;

    public MusicBrainzClient(
            @Qualifier("musicBrainzRestClient") RestClient restClient,
            MusicBrainzRequestGate requestGate
    ) {
        this.restClient = restClient;
        this.requestGate = requestGate;
    }

    @Override
    public List<RecordingCandidate> searchByIsrc(String isrc) {
        IsrcResponse response = get(
                "/isrc/{isrc}?inc=artist-credits&fmt=json",
                IsrcResponse.class,
                isrc
        );
        return response == null ? List.of() : candidates(response.recordings());
    }

    @Override
    public List<RecordingCandidate> searchByTitleAndArtists(String title, List<String> artists) {
        StringBuilder query = new StringBuilder("recording:\"").append(escape(title)).append("\"");
        for (String artist : artists) {
            query.append(" AND artist:\"").append(escape(artist)).append("\"");
        }
        return searchRecordings(query.toString());
    }

    @Override
    public RecordingDetails getRecording(String recordingId) {
        RecordingDto response = get(
                "/recording/{id}?inc=artist-credits+genres&fmt=json",
                RecordingDto.class,
                recordingId
        );
        if (response == null) {
            return new RecordingDetails(recordingId, List.of(), List.of());
        }
        return recordingDetails(response);
    }

    @Override
    public RecordingDetails getRecordingReleaseGroups(String recordingId) {
        RecordingDto response = get(
                "/recording/{id}?inc=artist-credits+releases&fmt=json",
                RecordingDto.class,
                recordingId
        );
        if (response == null) {
            return new RecordingDetails(recordingId, List.of(), List.of());
        }
        return recordingDetails(response);
    }

    private RecordingDetails recordingDetails(RecordingDto response) {
        LinkedHashMap<String, ReleaseGroupCandidate> groups = new LinkedHashMap<>();
        safe(response.releases()).forEach(release -> {
            ReleaseGroupDto group = release.releaseGroup();
            if (group == null || group.id() == null) {
                return;
            }
            List<ArtistCreditDto> credits = group.artistCredit() == null || group.artistCredit().isEmpty()
                    ? release.artistCredit()
                    : group.artistCredit();
            groups.putIfAbsent(group.id(), new ReleaseGroupCandidate(
                    group.id(),
                    group.title(),
                    year(group.firstReleaseDate() == null ? release.date() : group.firstReleaseDate()),
                    artistNames(credits)
            ));
        });
        return new RecordingDetails(response.id(), genres(response.genres()), List.copyOf(groups.values()));
    }

    @Override
    public ReleaseGroupDetails getReleaseGroup(String releaseGroupId) {
        ReleaseGroupDto response = get(
                "/release-group/{id}?inc=artist-credits+genres&fmt=json",
                ReleaseGroupDto.class,
                releaseGroupId
        );
        return new ReleaseGroupDetails(
                response == null ? releaseGroupId : response.id(),
                response == null ? List.of() : genres(response.genres())
        );
    }

    private List<RecordingCandidate> searchRecordings(String query) {
        SearchResponse response;
        try {
            requestGate.awaitPermission();
            response = restClient.get()
                    .uri(uri -> uri.path("/recording")
                            .queryParam("query", query)
                            .queryParam("limit", 100)
                            .queryParam("fmt", "json")
                            .build())
                    .retrieve()
                    .body(SearchResponse.class);
        } catch (RestClientException failure) {
            throw ExternalHttpFailureTranslator.translate("MusicBrainz", failure);
        }
        return response == null ? List.of() : candidates(response.recordings());
    }

    private List<RecordingCandidate> candidates(List<RecordingDto> recordings) {
        return safe(recordings).stream()
                .filter(recording -> recording.id() != null && !recording.id().isBlank())
                .map(this::candidate)
                .toList();
    }

    private RecordingCandidate candidate(RecordingDto recording) {
        return new RecordingCandidate(
                recording.id(), recording.title(), recording.length(), artistNames(recording.artistCredit())
        );
    }

    private <T> T get(String uri, Class<T> type, Object... variables) {
        try {
            requestGate.awaitPermission();
            return restClient.get().uri(uri, variables).retrieve().body(type);
        } catch (RestClientException failure) {
            throw ExternalHttpFailureTranslator.translate("MusicBrainz", failure);
        }
    }

    private String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private List<String> artistNames(List<ArtistCreditDto> credits) {
        return safe(credits).stream()
                .map(credit -> credit.name() != null
                        ? credit.name()
                        : credit.artist() == null ? null : credit.artist().name())
                .filter(name -> name != null && !name.isBlank())
                .toList();
    }

    private List<Genre> genres(List<GenreDto> values) {
        return safe(values).stream().map(value -> new Genre(value.name(), value.count())).toList();
    }

    private Integer year(String date) {
        if (date == null || date.length() < 4) {
            return null;
        }
        try {
            return Integer.valueOf(date.substring(0, 4));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private <T> List<T> safe(List<T> values) {
        return values == null ? List.of() : values;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record SearchResponse(List<RecordingDto> recordings) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record IsrcResponse(List<RecordingDto> recordings) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record RecordingDto(
            String id,
            String title,
            Integer length,
            @JsonProperty("artist-credit") List<ArtistCreditDto> artistCredit,
            List<GenreDto> genres,
            List<ReleaseDto> releases
    ) {
}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ReleaseDto(
            String date,
            @JsonProperty("artist-credit") List<ArtistCreditDto> artistCredit,
            @JsonProperty("release-group") ReleaseGroupDto releaseGroup
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ReleaseGroupDto(
            String id,
            String title,
            @JsonProperty("first-release-date") String firstReleaseDate,
            @JsonProperty("artist-credit") List<ArtistCreditDto> artistCredit,
            List<GenreDto> genres
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ArtistCreditDto(String name, ArtistDto artist) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ArtistDto(String name) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record GenreDto(String name, Integer count) {
    }
}
