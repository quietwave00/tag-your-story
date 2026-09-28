package com.tagnote.application.enrichment.matching;

import com.tagnote.application.catalog.importer.model.ImportedAlbum;
import com.tagnote.application.catalog.importer.model.ImportedArtist;
import com.tagnote.application.enrichment.matching.model.DiscogsCatalogData.AlbumCandidate;
import com.tagnote.application.enrichment.matching.model.DiscogsCatalogData.AlbumDetails;
import com.tagnote.application.enrichment.matching.model.DiscogsCatalogData.EntityType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DiscogsAlbumMatchingServiceTest {

    private final DiscogsAlbumMatchingService service = new DiscogsAlbumMatchingService(
            discogsNormalizer()
    );

    private static DiscogsAlbumIdentityNormalizer discogsNormalizer() {
        MusicEntityNameNormalizer nameNormalizer = new MusicEntityNameNormalizer();
        return new DiscogsAlbumIdentityNormalizer(
                nameNormalizer,
                new MusicEditionTitleNormalizer(nameNormalizer)
        );
    }

    @Test
    void title_artist가_exact인_후보가_복수이면_year로_좁힌다() {
        AlbumCandidate accepted = new AlbumCandidate(
                1L, EntityType.MASTER, "Ａｌｂｕｍ", 2026, List.of("Second Artist")
        );

        assertThat(service.matchingCandidates(album(), List.of(
                accepted,
                new AlbumCandidate(2L, EntityType.RELEASE, "Album", 2025, List.of("First Artist"))
        ))).containsExactly(accepted);
        assertThat(service.validates(album(), new AlbumDetails(
                1L, EntityType.MASTER, "Album", 2026, List.of("First Artist"), List.of(), List.of()
        ))).isTrue();
    }

    @Test
    void 유일한_title_artist_후보는_year가_달라도_채택한다() {
        AlbumCandidate accepted = new AlbumCandidate(
                1L, EntityType.MASTER, "Album", 1975, List.of("First Artist")
        );

        assertThat(service.matchingCandidates(album(), List.of(accepted))).containsExactly(accepted);
    }

    @Test
    void exact_후보가_복수이면_모두_반환한다() {
        assertThat(service.matchingCandidates(album(), List.of(
                new AlbumCandidate(1L, EntityType.MASTER, "Album", 2026, List.of("First Artist")),
                new AlbumCandidate(2L, EntityType.MASTER, "Album", 2026, List.of("First Artist"))
        ))).hasSize(2);
    }

    @Test
    void Queen_2011_remaster_표시를_제거하고_Discogs_master를_매칭한다() {
        ImportedAlbum queenAlbum = ImportedAlbum.of(
                2L,
                "spotify-album",
                "A Night At The Opera (2011 Remaster)",
                1975,
                List.of(ImportedArtist.of(1L, "queen", "Queen", 0))
        );
        AlbumCandidate master = new AlbumCandidate(
                5863L,
                EntityType.MASTER,
                "A Night At The Opera",
                1975,
                List.of("Queen (2)")
        );

        assertThat(service.searchTitle(queenAlbum)).isEqualTo("A Night At The Opera");
        assertThat(service.matchingCandidates(queenAlbum, List.of(master))).containsExactly(master);
        assertThat(service.validates(queenAlbum, new AlbumDetails(
                5863L,
                EntityType.MASTER,
                "A Night At The Opera",
                1975,
                List.of("Queen (2)"),
                List.of(),
                List.of()
        ))).isTrue();
    }

    @Test
    void 원문_title_exact_후보가_있으면_canonical_title_후보보다_우선한다() {
        ImportedAlbum deluxeAlbum = ImportedAlbum.of(
                2L,
                "spotify-album",
                "Album (Deluxe Edition)",
                2026,
                List.of(ImportedArtist.of(1L, "artist", "Artist", 0))
        );
        AlbumCandidate exact = new AlbumCandidate(
                1L, EntityType.MASTER, "Album (Deluxe Edition)", 2026, List.of("Artist")
        );
        AlbumCandidate canonicalOnly = new AlbumCandidate(
                2L, EntityType.MASTER, "Album", 2026, List.of("Artist")
        );

        assertThat(service.matchingCandidates(deluxeAlbum, List.of(canonicalOnly, exact)))
                .containsExactly(exact);
    }

    private ImportedAlbum album() {
        return ImportedAlbum.of(
                1L,
                "spotify-album",
                "Album",
                2026,
                List.of(
                        ImportedArtist.of(1L, "artist-1", "First Artist", 0),
                        ImportedArtist.of(2L, "artist-2", "Second Artist", 1)
                )
        );
    }
}
