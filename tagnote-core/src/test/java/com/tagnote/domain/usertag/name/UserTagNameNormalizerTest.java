package com.tagnote.domain.usertag.name;

import com.tagnote.core.domain.usertag.name.NormalizedUserTagName;
import com.tagnote.core.domain.usertag.name.UserTagNameNormalizer;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserTagNameNormalizerTest {

    private final UserTagNameNormalizer normalizer = new UserTagNameNormalizer();

    @Test
    void 공백_대소문자_Unicode_NFKC를_정규화한다() {
        NormalizedUserTagName fullWidth = normalizer.normalize("  ＪＡＺＺ\t  Mix  ");

        assertThat(fullWidth.value()).isEqualTo("jazz mix");
        assertThat(normalizer.normalize("jazz mix")).isEqualTo(fullWidth);
    }

    @Test
    void 의미있는_punctuation은_보존한다() {
        assertThat(normalizer.normalize("R&B").value()).isEqualTo("r&b");
        assertThat(normalizer.normalize("2-Step").value()).isEqualTo("2-step");
    }

    @Test
    void blank와_null은_거부한다() {
        assertThatThrownBy(() -> normalizer.normalize(" \t "))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> normalizer.normalize(null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
