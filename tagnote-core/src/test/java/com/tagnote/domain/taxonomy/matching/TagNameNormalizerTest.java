package com.tagnote.domain.taxonomy.matching;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TagNameNormalizerTest {

    private final TagNameNormalizer normalizer = new TagNameNormalizer();

    @Test
    void 공백과_대소문자와_Unicode를_정규화한다() {
        NormalizedTagName composed = normalizer.normalize("  CAFÉ\t  HOUSE  ");
        NormalizedTagName decomposed = normalizer.normalize("cafe\u0301 house");

        assertThat(composed).isEqualTo(new NormalizedTagName("café house"));
        assertThat(decomposed).isEqualTo(composed);
    }

    @Test
    void 의미_있는_punctuation은_보존한다() {
        assertThat(normalizer.normalize("R&B").value()).isEqualTo("r&b");
        assertThat(normalizer.normalize("2-Step").value()).isEqualTo("2-step");
        assertThat(normalizer.normalize("Drum 'n' Bass").value()).isEqualTo("drum 'n' bass");
    }

    @Test
    void blank_원본은_거부한다() {
        assertThatThrownBy(() -> normalizer.normalize(" \t "))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> normalizer.normalize(null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
