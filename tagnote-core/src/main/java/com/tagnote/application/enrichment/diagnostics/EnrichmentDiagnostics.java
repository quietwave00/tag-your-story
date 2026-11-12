package com.tagnote.application.enrichment.diagnostics;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tagnote.application.catalog.importer.model.ImportedAlbum;
import com.tagnote.application.catalog.importer.model.ImportedArtist;
import com.tagnote.application.catalog.importer.model.ImportedTrack;
import com.tagnote.application.catalog.detail.CatalogDetailService;
import com.tagnote.application.catalog.detail.model.SpotifyAlbumDetail;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Diagnostic context is copied explicitly into provider workers; no HTTP credentials enter events. */
@Component
public final class EnrichmentDiagnostics {
    private static final ThreadLocal<Context> CONTEXT = new ThreadLocal<>();
    private static final Object LOCK = new Object();
    private static final ObjectMapper JSON = new ObjectMapper();
    private static volatile Path file = Path.of("logs", "enrichment-diagnostics.jsonl");
    private static final long MAX_BYTES = 10L * 1024 * 1024;
    private static volatile boolean enabled;

    public EnrichmentDiagnostics(@Value("${tag.enrichment.diagnostics.enabled:false}") boolean configured,
                                 @Value("${tag.enrichment.diagnostics.file:logs/enrichment-diagnostics.jsonl}") String path) {
        enabled = configured;
        file = Path.of(path);
    }

    public static boolean enabled() { return enabled; }
    public static Configuration configuration() { return new Configuration(enabled, file.toString()); }
    public static Context current() { return CONTEXT.get(); }

    public static Scope open(String mode, String subjectType) {
        Context previous = CONTEXT.get();
        if (!enabled) return () -> {};
        CONTEXT.set(new Context(UUID.randomUUID().toString(), mode, subjectType));
        return () -> restore(previous);
    }

    public static Scope install(Context context) {
        Context previous = CONTEXT.get();
        if (context != null) CONTEXT.set(context);
        else CONTEXT.remove();
        return () -> restore(previous);
    }

    private static void restore(Context previous) {
        if (previous == null) CONTEXT.remove();
        else CONTEXT.set(previous);
    }

    public static void event(String type, Map<String, ?> fields) {
        Context context = CONTEXT.get();
        if (!enabled || context == null) return;
        try {
            Map<String, Object> event = new LinkedHashMap<>();
            event.put("time", Instant.now().toString());
            event.put("runId", context.runId());
            event.put("mode", context.mode());
            event.put("subjectType", context.subjectType());
            event.put("event", type);
            event.putAll(fields);
            byte[] line = (JSON.writeValueAsString(event) + "\n").getBytes(StandardCharsets.UTF_8);
            synchronized (LOCK) {
                if (file.getParent() != null) Files.createDirectories(file.getParent());
                if (Files.exists(file) && Files.size(file) + line.length > MAX_BYTES) rotate();
                Files.write(file, line, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            }
        } catch (Exception ignored) {
            // Diagnostics cannot change the import result, including when the disk is full.
        }
    }

    private static void rotate() throws java.io.IOException {
        for (int index = 4; index >= 1; index--) {
            Path from = Path.of(file + "." + index);
            Path to = Path.of(file + "." + (index + 1));
            if (Files.exists(from)) Files.move(from, to, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
        Files.move(file, Path.of(file + ".1"), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
    }

    public static void started(ImportedTrack track) {
        event("import_started", Map.of("catalogTrackId", track.getCatalogTrackId(),
                "spotifyTrackId", track.getSpotifyTrackId(), "title", track.getTitle(),
                "trackArtists", names(track.getArtists()), "isrcPresent", track.getIsrc() != null,
                "durationMs", track.getDurationMs() == null ? "UNKNOWN" : track.getDurationMs(),
                "album", albumFields(track.getAlbum())));
    }

    public static void started(ImportedAlbum album) {
        event("import_started", albumFields(album));
    }

    public static void release(ImportedAlbum album, CatalogDetailService details) {
        if (!enabled || details == null) return;
        try {
            SpotifyAlbumDetail spotify = details.getAlbum(album.getSpotifyAlbumId());
            Map<String, Object> fields = new LinkedHashMap<>();
            fields.put("spotifyAlbumId", album.getSpotifyAlbumId());
            fields.put("albumType", spotify.spotifyAlbumType());
            fields.put("totalTracks", spotify.totalTracks());
            fields.put("releaseType", spotify.releaseType() == null ? "UNKNOWN" : spotify.releaseType().name());
            event("release_classified", fields);
        } catch (Exception failure) {
            event("release_classified", Map.of("spotifyAlbumId", album.getSpotifyAlbumId(),
                    "releaseType", "UNKNOWN", "reason", failure.getClass().getSimpleName()));
        }
    }

    public static Map<String, Object> albumFields(ImportedAlbum album) {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("catalogAlbumId", album.getAlbumId());
        fields.put("spotifyAlbumId", album.getSpotifyAlbumId());
        fields.put("albumTitle", album.getTitle());
        fields.put("albumArtists", names(album.getArtists()));
        fields.put("releaseYear", album.getReleaseYear());
        return fields;
    }

    private static List<String> names(List<ImportedArtist> artists) {
        return artists.stream().map(ImportedArtist::getName).limit(20).toList();
    }

    public static Map<String, Object> bounded(List<?> values) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalCount", values.size());
        result.put("truncated", values.size() > 20);
        result.put("items", new ArrayList<>(values.subList(0, Math.min(values.size(), 20))));
        return result;
    }

    public static String safe(String value) { return value == null ? "" : value; }

    public record Context(String runId, String mode, String subjectType) {}
    public record Configuration(boolean enabled, String file) {}
    public interface Scope extends AutoCloseable { @Override void close(); }
}
