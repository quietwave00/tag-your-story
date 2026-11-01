package com.tagnote.infrastructure.external.spotify;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

final class SpotifyFixtures {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private SpotifyFixtures() {}

    static <T> T read(String json, Class<T> type) {
        try { return MAPPER.readValue(json, type); }
        catch (Exception failure) { throw new AssertionError(failure); }
    }

    static <T> T read(String json, TypeReference<T> type) {
        try { return MAPPER.readValue(json, type); }
        catch (Exception failure) { throw new AssertionError(failure); }
    }
}
