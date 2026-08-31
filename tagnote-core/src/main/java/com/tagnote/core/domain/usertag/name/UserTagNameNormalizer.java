package com.tagnote.core.domain.usertag.name;

import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.Locale;

@Component
public class UserTagNameNormalizer {

    public NormalizedUserTagName normalize(String rawName) {
        if (rawName == null || rawName.isBlank()) {
            throw new IllegalArgumentException("Raw user tag name must not be blank");
        }

        String normalized = Normalizer.normalize(rawName, Normalizer.Form.NFKC)
                .trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", " ");

        return new NormalizedUserTagName(normalized);
    }
}
