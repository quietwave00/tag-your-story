package com.tagnote.application.enrichment.matching;

import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.regex.Pattern;

@Component
public class DiscogsAlbumIdentityNormalizer {

    private static final Pattern DISCOGS_ARTIST_SUFFIX = Pattern.compile("\\s+\\([2-9][0-9]*\\)\\s*$");
    private static final Pattern TRAILING_SLASH_LIST = Pattern.compile("^(.*?)\\s*\\(([^()]*/[^()]*)\\)\\s*$");

    private final MusicEntityNameNormalizer normalizer;
    private final MusicEditionTitleNormalizer editionTitleNormalizer;

    public DiscogsAlbumIdentityNormalizer(
            MusicEntityNameNormalizer normalizer,
            MusicEditionTitleNormalizer editionTitleNormalizer
    ) {
        this.normalizer = normalizer;
        this.editionTitleNormalizer = editionTitleNormalizer;
    }

    public String searchTitle(String value) {
        return editionTitleNormalizer.canonicalTitle(value);
    }

    public boolean rawTitleExact(String left, String right) {
        return exactComparable(left, right);
    }

    public boolean canonicalTitleExact(String left, String right) {
        return exactComparable(searchTitle(left), searchTitle(right));
    }

    public boolean artistExact(String importedArtist, String discogsArtist) {
        String candidate = discogsArtist == null
                ? null
                : DISCOGS_ARTIST_SUFFIX.matcher(discogsArtist).replaceFirst("");
        return exactComparable(importedArtist, candidate);
    }

    public Optional<String> boxSetBaseTitle(String value) {
        if (value == null) {
            return Optional.empty();
        }
        var matcher = TRAILING_SLASH_LIST.matcher(value.trim());
        if (!matcher.matches()) {
            return Optional.empty();
        }
        String baseTitle = matcher.group(1).trim();
        boolean validList = !baseTitle.isEmpty()
                && java.util.Arrays.stream(matcher.group(2).split("/", -1))
                .map(String::trim)
                .allMatch(part -> !part.isEmpty());
        return validList ? Optional.of(baseTitle) : Optional.empty();
    }

    private String normalizeComparable(String value) {
        String normalized = normalizer.normalize(value);
        if (normalized == null) {
            return "";
        }
        return normalized
                .replace('\u2018', '\'')
                .replace('\u2019', '\'')
                .replace('\u201c', '"')
                .replace('\u201d', '"')
                .replace('\u2013', '-')
                .replace('\u2014', '-');
    }

    private boolean exactComparable(String left, String right) {
        String normalizedLeft = normalizeComparable(left);
        return !normalizedLeft.isEmpty() && normalizedLeft.equals(normalizeComparable(right));
    }
}
