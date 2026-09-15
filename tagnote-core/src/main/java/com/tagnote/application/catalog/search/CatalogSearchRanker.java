package com.tagnote.application.catalog.search;

import com.tagnote.application.catalog.search.model.CatalogSearchItem;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.text.Normalizer;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class CatalogSearchRanker {

    static List<CatalogSearchItem> rank(String keyword, List<CatalogSearchItem> items) {
        String normalizedKeyword = normalize(keyword);
        return items.stream()
                .sorted(comparator(normalizedKeyword))
                .toList();
    }

    private static Comparator<CatalogSearchItem> comparator(String keyword) {
        return Comparator
                .comparing((CatalogSearchItem item) -> exact(keyword, item.getTitle())).reversed()
                .thenComparing(item -> prefix(keyword, item.getTitle()), Comparator.reverseOrder())
                .thenComparing(item -> similarity(keyword, item.getTitle()), Comparator.reverseOrder())
                .thenComparing(item -> similarity(keyword, item.getArtistName()), Comparator.reverseOrder())
                .thenComparingInt(CatalogSearchItem::getProviderRank)
                .thenComparingInt(item -> item.getSubjectType().ordinal())
                .thenComparing(CatalogSearchItem::getSpotifyId);
    }

    private static boolean exact(String keyword, String value) {
        return !keyword.isEmpty() && keyword.equals(normalize(value));
    }

    private static boolean prefix(String keyword, String value) {
        return !keyword.isEmpty() && normalize(value).startsWith(keyword);
    }

    private static double similarity(String keyword, String value) {
        String normalizedValue = normalize(value);
        int longestLength = Math.max(keyword.length(), normalizedValue.length());
        if (longestLength == 0) {
            return 1.0;
        }
        return 1.0 - ((double) levenshteinDistance(keyword, normalizedValue) / longestLength);
    }

    private static int levenshteinDistance(String left, String right) {
        int[] previous = new int[right.length() + 1];
        int[] current = new int[right.length() + 1];
        for (int column = 0; column <= right.length(); column++) {
            previous[column] = column;
        }

        for (int row = 1; row <= left.length(); row++) {
            current[0] = row;
            for (int column = 1; column <= right.length(); column++) {
                int substitutionCost = left.charAt(row - 1) == right.charAt(column - 1) ? 0 : 1;
                current[column] = Math.min(
                        Math.min(current[column - 1] + 1, previous[column] + 1),
                        previous[column - 1] + substitutionCost
                );
            }
            int[] swap = previous;
            previous = current;
            current = swap;
        }
        return previous[right.length()];
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return Normalizer.normalize(value, Normalizer.Form.NFKC)
                .toLowerCase(Locale.ROOT)
                .trim()
                .replaceAll("\\s+", " ");
    }
}
