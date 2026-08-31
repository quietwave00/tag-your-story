package com.tagnote.domain.taxonomy.matching;

import java.text.Normalizer;
import java.util.Locale;

import org.springframework.stereotype.Component;

@Component
public class TagNameNormalizer {

    public NormalizedTagName normalize(String rawName) {
        if (rawName == null || rawName.isBlank()) {
            throw new IllegalArgumentException("Raw tag name must not be blank");
        }

        String normalized = Normalizer.normalize(rawName, Normalizer.Form.NFKC)
                .trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", " ");

        return new NormalizedTagName(normalized);
    }
}
