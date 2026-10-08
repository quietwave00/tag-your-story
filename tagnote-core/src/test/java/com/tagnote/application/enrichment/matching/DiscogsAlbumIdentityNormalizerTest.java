package com.tagnote.application.enrichment.matching;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DiscogsAlbumIdentityNormalizerTest {

    private final MusicEntityNameNormalizer nameNormalizer = new MusicEntityNameNormalizer();
    private final DiscogsAlbumIdentityNormalizer normalizer = new DiscogsAlbumIdentityNormalizer(
            nameNormalizer,
            new MusicEditionTitleNormalizer(nameNormalizer)
    );

    @Test
    void 허용된_trailing_판본표시만_검색제목에서_제거한다() {
        Map<String, String> cases = Map.ofEntries(
                Map.entry("A Night At The Opera (2011 Remaster)", "A Night At The Opera"),
                Map.entry("Album [Remastered 2011]", "Album"),
                Map.entry("Album (Remastered Deluxe Edition)", "Album"),
                Map.entry("Album - Super Deluxe Edition", "Album"),
                Map.entry("Album - Part One - Deluxe Edition", "Album - Part One"),
                Map.entry("Album (Deluxe)", "Album"),
                Map.entry("Album (30th Anniversary Deluxe Edition)", "Album"),
                Map.entry("Album (40th Anniversary Remaster)", "Album"),
                Map.entry("Album (Bonus Tracks Version)", "Album"),
                Map.entry("Album (International Version)", "Album"),
                Map.entry("Album (2011 Reissue)", "Album"),
                Map.entry("Album (Original Mono Mix)", "Album"),
                Map.entry("Album (Explicit)", "Album"),
                Map.entry("Album (Deluxe Edition) (2011 Remaster)", "Album")
        );

        cases.forEach((input, expected) ->
                assertThat(normalizer.searchTitle(input)).as(input).isEqualTo(expected)
        );
    }

    @Test
    void 음악적_의미가_달라지는_trailing_표시는_보존한다() {
        for (String title : new String[]{
                "Album (Live At Wembley)",
                "Album (The Remixes)",
                "Album (Acoustic)",
                "Album (Instrumental)",
                "Album (Original Motion Picture Soundtrack)",
                "Album (Taylor's Version)",
                "Album (Demo)",
                "Album - Act II"
        }) {
            assertThat(normalizer.searchTitle(title)).as(title).isEqualTo(title);
        }
    }

    @Test
    void Discogs_artist_suffix와_typographic_punctuation만_비교용으로_정규화한다() {
        assertThat(normalizer.artistExact("John Smith", "John Smith (2)")).isTrue();
        assertThat(normalizer.artistExact("John Smith", "John Smith Jr. (2)")).isFalse();
        assertThat(normalizer.canonicalTitleExact("What’s Here – There", "What's Here - There")).isTrue();
    }
}
