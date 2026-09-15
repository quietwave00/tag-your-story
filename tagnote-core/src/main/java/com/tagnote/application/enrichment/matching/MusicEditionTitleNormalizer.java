package com.tagnote.application.enrichment.matching;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class MusicEditionTitleNormalizer {

    private static final Pattern TRAILING_PARENTHESIS = Pattern.compile("^(.*?)\\s*\\(([^()]*)\\)\\s*$");
    private static final Pattern TRAILING_BRACKET = Pattern.compile("^(.*?)\\s*\\[([^\\[\\]]*)\\]\\s*$");
    private static final Pattern TRAILING_SEPARATOR = Pattern.compile(
            "^(.*)(?:\\s+[-–—]\\s+)([^\\r\\n]+)$"
    );
    private static final List<Pattern> EDITION_QUALIFIERS = List.of(
            qualifier("(?:[0-9]{4}\\s+)?(?:digital\\s+)?remaster(?:ed)?"
                    + "(?:\\s+[0-9]{4})?(?:\\s+(?:edition|version))?"),
            qualifier("(?:[0-9]{4}\\s+)?remaster(?:ed)?\\s+(?:super\\s+)?"
                    + "(?:deluxe|expanded|special|legacy|anniversary)"
                    + "(?:\\s+(?:edition|version))?"),
            qualifier("(?:(?:[0-9]+(?:st|nd|rd|th)|[0-9]{4})\\s+anniversary\\s+)?"
                    + "(?:super\\s+)?(?:deluxe|expanded)(?:\\s+remaster(?:ed)?)?"
                    + "(?:\\s+(?:edition|version))?"),
            qualifier("(?:special|legacy|anniversary)(?:\\s+remaster(?:ed)?)?"
                    + "\\s+(?:edition|version)"),
            qualifier("(?:[0-9]+(?:st|nd|rd|th)|[0-9]{4})\\s+anniversary"
                    + "(?:\\s+remaster(?:ed)?)?(?:\\s+(?:edition|version))?"),
            qualifier("bonus\\s+track(?:s)?\\s+(?:edition|version)"),
            qualifier("international\\s+(?:edition|version)"),
            qualifier("(?:[0-9]{4}\\s+)?(?:reissue|re-release)(?:\\s+[0-9]{4})?"
                    + "(?:\\s+(?:edition|version))?"),
            qualifier("(?:original\\s+)?(?:mono|stereo)(?:\\s+(?:mix|edition|version))?"),
            qualifier("explicit(?:\\s+(?:edition|version))?"),
            qualifier("clean\\s+(?:edition|version)")
    );

    private final MusicEntityNameNormalizer normalizer;

    public MusicEditionTitleNormalizer(MusicEntityNameNormalizer normalizer) {
        this.normalizer = normalizer;
    }

    public String canonicalTitle(String value) {
        if (value == null) {
            return null;
        }
        String current = value.trim();
        while (true) {
            String stripped = stripOneTrailingQualifier(current);
            if (stripped.equals(current)) {
                return current;
            }
            current = stripped;
        }
    }

    private String stripOneTrailingQualifier(String value) {
        for (Pattern container : List.of(TRAILING_PARENTHESIS, TRAILING_BRACKET, TRAILING_SEPARATOR)) {
            Matcher matcher = container.matcher(value);
            if (matcher.matches() && isEditionQualifier(matcher.group(2))) {
                return matcher.group(1).trim();
            }
        }
        return value;
    }

    private boolean isEditionQualifier(String value) {
        String normalized = normalizer.normalize(value);
        if (normalized == null) {
            return false;
        }
        String comparable = normalized
                .replace('\u2018', '\'')
                .replace('\u2019', '\'')
                .replace('\u201c', '"')
                .replace('\u201d', '"')
                .replace('\u2013', '-')
                .replace('\u2014', '-');
        return EDITION_QUALIFIERS.stream().anyMatch(pattern -> pattern.matcher(comparable).matches());
    }

    private static Pattern qualifier(String expression) {
        return Pattern.compile("^(?:" + expression + ")$", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    }
}
