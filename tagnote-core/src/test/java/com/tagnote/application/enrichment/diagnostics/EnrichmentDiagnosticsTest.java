package com.tagnote.application.enrichment.diagnostics;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tagnote.application.catalog.detail.CatalogDetailService;
import com.tagnote.application.catalog.detail.model.SpotifyAlbumDetail;
import com.tagnote.application.catalog.importer.model.ImportedAlbum;
import com.tagnote.application.catalog.importer.model.ImportedArtist;
import com.tagnote.application.enrichment.matching.DiscogsAlbumIdentityNormalizer;
import com.tagnote.application.enrichment.matching.DiscogsAlbumMatchingService;
import com.tagnote.application.enrichment.matching.MusicEditionTitleNormalizer;
import com.tagnote.application.enrichment.matching.MusicEntityNameNormalizer;
import com.tagnote.application.enrichment.matching.model.DiscogsCatalogData.AlbumCandidate;
import com.tagnote.application.enrichment.matching.model.DiscogsCatalogData.EntityType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class EnrichmentDiagnosticsTest {
    @TempDir Path directory;
    private EnrichmentDiagnostics.Configuration original;

    @BeforeEach
    void 기존_설정을_기억한다() { original = EnrichmentDiagnostics.configuration(); }

    @AfterEach
    void 설정을_복원한다() {
        new EnrichmentDiagnostics(original.enabled(), original.file());
    }

    @Test
    void 비활성화하면_파일을_생성하지_않는다() {
        Path file = directory.resolve("disabled.jsonl");
        new EnrichmentDiagnostics(false, file.toString());
        try (EnrichmentDiagnostics.Scope ignored = EnrichmentDiagnostics.open("IMPORT_PIPELINE", "TRACK")) {
            EnrichmentDiagnostics.event("provider_completed", Map.of("status", "SUCCESS"));
        }
        assertThat(file).doesNotExist();
    }

    @Test
    void 파일_기록_실패가_호출자를_중단하지_않는다() {
        new EnrichmentDiagnostics(true, directory.toString());
        try (EnrichmentDiagnostics.Scope ignored = EnrichmentDiagnostics.open("IMPORT_PIPELINE", "TRACK")) {
            EnrichmentDiagnostics.event("import_started", Map.of("title", "Track"));
            assertThat(EnrichmentDiagnostics.current()).isNotNull();
        }
    }

    @Test
    void 병렬_작업의_runId가_격리되고_정리된다() throws Exception {
        Path file = directory.resolve("events.jsonl");
        new EnrichmentDiagnostics(true, file.toString());
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            String firstId;
            try (EnrichmentDiagnostics.Scope ignored = EnrichmentDiagnostics.open("IMPORT_PIPELINE", "TRACK")) {
                firstId = EnrichmentDiagnostics.current().runId();
                EnrichmentDiagnostics.Context context = EnrichmentDiagnostics.current();
                executor.submit(() -> {
                    try (EnrichmentDiagnostics.Scope worker = EnrichmentDiagnostics.install(context)) {
                        EnrichmentDiagnostics.event("provider_completed", Map.of("status", "SUCCESS"));
                    }
                }).get();
            }
            assertThat(executor.submit(EnrichmentDiagnostics::current).get()).isNull();
            try (EnrichmentDiagnostics.Scope ignored = EnrichmentDiagnostics.open("IMPORT_PIPELINE", "ALBUM")) {
                EnrichmentDiagnostics.event("import_started", Map.of("title", "Album"));
                assertThat(EnrichmentDiagnostics.current().runId()).isNotEqualTo(firstId);
            }
        } finally {
            executor.shutdownNow();
        }
        List<String> lines = Files.readAllLines(file);
        assertThat(lines).hasSize(2);
        ObjectMapper mapper = new ObjectMapper();
        JsonNode first = mapper.readTree(lines.get(0));
        JsonNode second = mapper.readTree(lines.get(1));
        assertThat(first.get("runId").asText()).isNotEqualTo(second.get("runId").asText());
        assertThat(first.get("subjectType").asText()).isEqualTo("TRACK");
        assertThat(second.get("subjectType").asText()).isEqualTo("ALBUM");
    }

    @Test
    void 기존_발매_분류를_사용하고_실패하면_UNKNOWN을_남긴다() throws Exception {
        Path file = directory.resolve("release.jsonl");
        new EnrichmentDiagnostics(true, file.toString());
        CatalogDetailService details = mock(CatalogDetailService.class);
        ImportedAlbum album = ImportedAlbum.of(1L, "spotify-album", "An EP", 2020, List.of());
        when(details.getAlbum("spotify-album")).thenReturn(new SpotifyAlbumDetail(
                "spotify-album", "An EP", null, "2020", 4, List.of(), "album", 600000L));
        try (EnrichmentDiagnostics.Scope ignored = EnrichmentDiagnostics.open("IMPORT_PIPELINE", "ALBUM")) {
            EnrichmentDiagnostics.release(album, details);
        }
        JsonNode classified = new ObjectMapper().readTree(Files.readAllLines(file).get(0));
        assertThat(classified.get("releaseType").asText()).isEqualTo("SINGLE_EP");

        when(details.getAlbum("spotify-album")).thenThrow(new IllegalStateException("Spotify unavailable"));
        try (EnrichmentDiagnostics.Scope ignored = EnrichmentDiagnostics.open("IMPORT_PIPELINE", "ALBUM")) {
            EnrichmentDiagnostics.release(album, details);
        }
        JsonNode failed = new ObjectMapper().readTree(Files.readAllLines(file).get(1));
        assertThat(failed.get("releaseType").asText()).isEqualTo("UNKNOWN");
        assertThat(failed.toString()).doesNotContain("Spotify unavailable");
    }

    @Test
    void 비활성화하면_Spotify_발매_정보를_읽지_않는다() {
        new EnrichmentDiagnostics(false, directory.resolve("disabled.jsonl").toString());
        CatalogDetailService details = mock(CatalogDetailService.class);
        EnrichmentDiagnostics.release(ImportedAlbum.of(1L, "id", "Album", 2020, List.of()), details);
        verifyNoInteractions(details);
    }

    @Test
    void Discogs_후보가_있어도_아티스트_필터에서_탈락하면_단계를_기록한다() throws Exception {
        Path file = directory.resolve("discogs.jsonl");
        new EnrichmentDiagnostics(true, file.toString());
        MusicEntityNameNormalizer names = new MusicEntityNameNormalizer();
        DiscogsAlbumMatchingService matching = new DiscogsAlbumMatchingService(
                new DiscogsAlbumIdentityNormalizer(names, new MusicEditionTitleNormalizer(names)));
        ImportedAlbum album = ImportedAlbum.of(1L, "album", "Album", 2020,
                List.of(ImportedArtist.of(2L, "artist", "Artist", 0)));
        try (EnrichmentDiagnostics.Scope ignored = EnrichmentDiagnostics.open("PROVIDER_ONLY", "ALBUM")) {
            assertThat(matching.matchingCandidates(album, List.of(
                    new AlbumCandidate(17L, EntityType.MASTER, "Album", 2020, List.of("Other")))))
                    .isEmpty();
        }
        JsonNode result = new ObjectMapper().readTree(Files.readAllLines(file).get(0));
        assertThat(result.get("rawCount").asInt()).isEqualTo(1);
        assertThat(result.get("artistCount").asInt()).isZero();
        assertThat(result.get("candidates").get("items").get(0).get("id").asLong()).isEqualTo(17L);
    }
}
