package com.tagnote.application.enrichment.matching;

import com.tagnote.application.catalog.importer.model.ImportedAlbum;
import com.tagnote.application.catalog.importer.model.ImportedArtist;
import com.tagnote.application.catalog.importer.model.ImportedTrack;
import com.tagnote.application.enrichment.matching.model.DiscogsCatalogData.SearchField;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DiscogsAlbumSearchPlanTest {

    private final MusicEntityNameNormalizer nameNormalizer = new MusicEntityNameNormalizer();
    private final DiscogsAlbumSearchPlan plan = new DiscogsAlbumSearchPlan(
            new DiscogsAlbumIdentityNormalizer(
                    nameNormalizer,
                    new MusicEditionTitleNormalizer(nameNormalizer)
            )
    );

    @Test
    void box_set은_원문_album_축약_album_track_순서로_검색한다() {
        var attempts = plan.attempts(track(
                "Creep",
                "5 Album Set (Pablo Honey/The Bends/OK Computer/Kid A/Amnesiac)"
        ));

        assertThat(attempts)
                .extracting(
                        attempt -> attempt.query().field(),
                        attempt -> attempt.query().value(),
                        attempt -> attempt.validationTitle()
                )
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(
                                SearchField.RELEASE_TITLE,
                                "5 Album Set (Pablo Honey/The Bends/OK Computer/Kid A/Amnesiac)",
                                "5 Album Set (Pablo Honey/The Bends/OK Computer/Kid A/Amnesiac)"
                        ),
                        org.assertj.core.groups.Tuple.tuple(
                                SearchField.RELEASE_TITLE, "5 Album Set", "5 Album Set"
                        ),
                        org.assertj.core.groups.Tuple.tuple(
                                SearchField.TRACK, "Creep", "5 Album Set"
                        )
                );
    }

    @Test
    void slash목록이_아닌_괄호는_축약하지_않는다() {
        var attempts = plan.attempts(track("Song", "Album (Live)"));

        assertThat(attempts)
                .extracting(attempt -> attempt.query().field(), attempt -> attempt.query().value())
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(SearchField.RELEASE_TITLE, "Album (Live)"),
                        org.assertj.core.groups.Tuple.tuple(SearchField.TRACK, "Song")
                );
    }

    private ImportedTrack track(String trackTitle, String albumTitle) {
        ImportedArtist artist = ImportedArtist.of(1L, "artist", "Radiohead", 0);
        return ImportedTrack.of(
                1L,
                "spotify-track",
                null,
                trackTitle,
                "ISRC",
                180_000,
                List.of(artist),
                ImportedAlbum.of(2L, "spotify-album", albumTitle, 2012, List.of(artist))
        );
    }
}
