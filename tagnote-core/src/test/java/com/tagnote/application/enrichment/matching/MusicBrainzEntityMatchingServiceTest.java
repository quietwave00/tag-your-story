package com.tagnote.application.enrichment.matching;

import com.tagnote.application.catalog.importer.model.ImportedAlbum;
import com.tagnote.application.catalog.importer.model.ImportedArtist;
import com.tagnote.application.catalog.importer.model.ImportedTrack;
import com.tagnote.application.enrichment.config.ExternalEnrichmentProperties;
import com.tagnote.application.enrichment.matching.model.MusicBrainzCatalogData.RecordingCandidate;
import com.tagnote.application.enrichment.matching.model.MusicBrainzCatalogData.ReleaseGroupCandidate;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MusicBrainzEntityMatchingServiceTest {

    private final ExternalEnrichmentProperties properties = new ExternalEnrichmentProperties();
    private final MusicBrainzEntityMatchingService service = new MusicBrainzEntityMatchingService(
            new MusicEntityNameNormalizer(), properties
    );

    @Test
    void 유일한_Isrc_후보는_artist가_일치하면_title과_duration이_달라도_채택한다() {
        RecordingCandidate candidate = new RecordingCandidate(
                "isrc-match", "Original Title", 240_000, List.of("Artist")
        );

        assertThat(service.matchRecordingByIsrc(track(), List.of(candidate))).contains(candidate);
        assertThat(service.matchRecordingByIsrc(track(), List.of(
                new RecordingCandidate("wrong-artist", "Track Name", 180_000, List.of("Other"))
        ))).isEmpty();
    }

    @Test
    void 복수_Isrc_후보는_title_artist_duration이_유일하게_일치해야_채택한다() {
        RecordingCandidate accepted = new RecordingCandidate(
                "accepted", "Ｔｒａｃｋ  Name", 183_000, List.of("Artist")
        );

        assertThat(service.matchRecordingByIsrc(track(), List.of(
                accepted,
                new RecordingCandidate("wrong", "Track Name", 183_001, List.of("Other"))
        ))).contains(accepted);
    }

    @Test
    void 복수_Isrc_후보가_모두_일치하면_duration이_가장_가까운_단일후보를_채택한다() {
        ImportedTrack creep = track("Creep", 235_640);
        RecordingCandidate closest = new RecordingCandidate(
                "70595637-9310-45f2-a266-58f8de4874a7", "Creep", 236_666, List.of("Artist")
        );

        assertThat(service.matchRecordingByIsrc(creep, List.of(
                closest,
                new RecordingCandidate(
                        "1f9f17c7-6085-4d52-9bd0-13a94cc449bc", "Creep", 237_680, List.of("Artist")
                )
        ))).contains(closest);
    }

    @Test
    void 복수_Isrc_후보의_최소_duration_차이가_동률이면_거부한다() {
        ImportedTrack track = track("Track Name", 180_000);

        assertThat(service.matchRecordingByIsrc(track, List.of(
                new RecordingCandidate("shorter", "Track Name", 179_000, List.of("Artist")),
                new RecordingCandidate("longer", "Track Name", 181_000, List.of("Artist"))
        ))).isEmpty();
    }

    @Test
    void duration_3000ms는_포함하고_초과하거나_동률이면_거부한다() {
        RecordingCandidate boundary = new RecordingCandidate(
                "boundary", "Track Name", 183_000, List.of("Artist")
        );
        assertThat(service.matchRecordingByMetadata(track(), List.of(boundary))).contains(boundary);

        assertThat(service.matchRecordingByMetadata(track(), List.of(
                boundary,
                new RecordingCandidate("same", "Track Name", 180_000, List.of("Artist"))
        ))).isEmpty();
        assertThat(service.matchRecordingByMetadata(track(), List.of(
                new RecordingCandidate("too-far", "Track Name", 183_001, List.of("Artist"))
        ))).isEmpty();
    }

    @Test
    void release_group은_title_artist_year가_맞는_유일후보만_채택한다() {
        ReleaseGroupCandidate accepted = new ReleaseGroupCandidate(
                "rg", "Album", 2026, List.of("Album Artist")
        );
        assertThat(service.matchReleaseGroup(track().getAlbum(), List.of(accepted))).contains(accepted);
        assertThat(service.matchReleaseGroup(track().getAlbum(), List.of(
                accepted,
                new ReleaseGroupCandidate("other", "Album", 2026, List.of("Album Artist"))
        ))).isEmpty();
    }

    private ImportedTrack track() {
        return track("Track Name", 180_000);
    }

    private ImportedTrack track(String title, int durationMs) {
        return ImportedTrack.of(
                1L,
                "spotify-track",
                null,
                title,
                "ISRC",
                durationMs,
                List.of(ImportedArtist.of(1L, "artist", "Artist", 0)),
                ImportedAlbum.of(
                        2L,
                        "spotify-album",
                        "Album",
                        2026,
                        List.of(ImportedArtist.of(2L, "album-artist", "Album Artist", 0))
                )
        );
    }
}
