package com.tagnote.core.domain.user;

import java.util.Locale;
import java.util.regex.Pattern;

public final class EmailNormalizer {
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private EmailNormalizer() {
    }

    public static String normalize(String email) {
        if (email == null) {
            throw new IllegalArgumentException("Email is required");
        }
        String normalized = email.trim().toLowerCase(Locale.ROOT);
        if (normalized.length() > 255 || !EMAIL.matcher(normalized).matches()) {
            throw new IllegalArgumentException("Invalid email");
        }
        return normalized;
    }
}
