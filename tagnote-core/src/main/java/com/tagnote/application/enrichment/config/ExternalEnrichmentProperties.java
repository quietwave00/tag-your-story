package com.tagnote.application.enrichment.config;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "tag.enrichment")
public class ExternalEnrichmentProperties {

    private int executorSize = 3;
    private long totalFirstLoadBudgetMs = 3000;
    private final MusicBrainz musicbrainz = new MusicBrainz();
    private final Discogs discogs = new Discogs();
    private final LastFm lastfm = new LastFm();
    private final Matching matching = new Matching();
    private final EvidenceConfidence evidenceConfidence = new EvidenceConfidence();

    @PostConstruct
    void validate() {
        requirePositive(executorSize, "Executor size");
        requirePositive(totalFirstLoadBudgetMs, "Total first-load budget");
        musicbrainz.validate();
        discogs.validate();
        lastfm.validate();
        matching.validate();
        evidenceConfidence.validate();
    }

    @Getter
    @Setter
    public static class HttpProvider {
        private boolean enabled;
        private String baseUrl;
        private int connectTimeoutMs = 500;
        private int readTimeoutMs = 2500;

        protected void validateBase(String name) {
            requirePositive(connectTimeoutMs, name + " connect timeout");
            requirePositive(readTimeoutMs, name + " read timeout");
            if (enabled) {
                requireText(baseUrl, name + " base URL");
            }
        }
    }

    @Getter
    @Setter
    public static class MusicBrainz extends HttpProvider {
        private long minimumRequestIntervalMs = 1000;
        private long completionSafetyMarginMs = 250;
        private String userAgent;

        private void validate() {
            validateBase("MusicBrainz");
            requirePositive(minimumRequestIntervalMs, "MusicBrainz minimum request interval");
            requirePositive(completionSafetyMarginMs, "MusicBrainz completion safety margin");
            if (isEnabled()) {
                requireText(userAgent, "MusicBrainz User-Agent");
            }
        }
    }

    @Getter
    @Setter
    public static class Discogs extends HttpProvider {
        private String userAgent;
        private String token;

        private void validate() {
            validateBase("Discogs");
            if (isEnabled()) {
                requireText(userAgent, "Discogs User-Agent");
                requireText(token, "Discogs token");
            }
        }
    }

    @Getter
    @Setter
    public static class LastFm extends HttpProvider {
        private String apiKey;
        private int maximumTagsPerSubject = 5;

        private void validate() {
            validateBase("Last.fm");
            requirePositive(maximumTagsPerSubject, "Last.fm maximum tags per subject");
            if (isEnabled()) {
                requireText(apiKey, "Last.fm API key");
            }
        }
    }

    @Getter
    @Setter
    public static class Matching {
        private int durationToleranceMs = 3000;

        private void validate() {
            if (durationToleranceMs < 0) {
                throw new IllegalArgumentException("Duration tolerance must not be negative");
            }
        }
    }

    @Getter
    @Setter
    public static class EvidenceConfidence {
        private double musicbrainzRecordingGenre = 0.90;
        private double musicbrainzReleaseGroupGenre = 0.75;
        private double discogsGenre = 0.70;
        private double discogsStyle = 0.85;
        private double lastfmTrackCommunityTag = 0.65;
        private double lastfmAlbumCommunityTag = 0.60;

        private void validate() {
            requireProbability(musicbrainzRecordingGenre, "MusicBrainz Recording genre confidence");
            requireProbability(musicbrainzReleaseGroupGenre, "MusicBrainz Release Group genre confidence");
            requireProbability(discogsGenre, "Discogs genre confidence");
            requireProbability(discogsStyle, "Discogs style confidence");
            requireProbability(lastfmTrackCommunityTag, "Last.fm Track community-tag confidence");
            requireProbability(lastfmAlbumCommunityTag, "Last.fm Album community-tag confidence");
        }
    }

    private static void requirePositive(long value, String name) {
        if (value <= 0) {
            throw new IllegalArgumentException(name + " must be positive");
        }
    }

    private static void requireProbability(double value, String name) {
        if (!Double.isFinite(value) || value < 0.0 || value > 1.0) {
            throw new IllegalArgumentException(name + " must be between 0.0 and 1.0");
        }
    }

    private static void requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank when enabled");
        }
    }
}
