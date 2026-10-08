package com.tagnote.application.enrichment.matching;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MusicEntityNameNormalizerTest {

    private final MusicEntityNameNormalizer normalizer = new MusicEntityNameNormalizer();

    @Test
    void Nfkc_case_trim과_연속공백만_정규화한다() {
        assertThat(normalizer.normalize("  ＴＲＡＣＫ\t  Name  ")).isEqualTo("track name");
    }

    @Test
    void punctuation과_edition표시는_임의로_제거하지_않는다() {
        assertThat(normalizer.exact("Track - Remix", "Track Remix")).isFalse();
        assertThat(normalizer.exact("Album (Deluxe)", "Album")).isFalse();
    }
}
