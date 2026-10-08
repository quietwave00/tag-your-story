package com.tagnote.application.enrichment.matching;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MusicEditionTitleNormalizerTest {

    private final MusicEditionTitleNormalizer normalizer = new MusicEditionTitleNormalizer(
            new MusicEntityNameNormalizer()
    );

    @Test
    void trailing_remaster를_제거한다() {
        assertThat(normalizer.canonicalTitle("Bohemian Rhapsody - Remastered 2011"))
                .isEqualTo("Bohemian Rhapsody");
    }

    @Test
    void live_remix_acoustic은_보존한다() {
        assertThat(normalizer.canonicalTitle("Track - Live At Wembley"))
                .isEqualTo("Track - Live At Wembley");
        assertThat(normalizer.canonicalTitle("Track (Remix)"))
                .isEqualTo("Track (Remix)");
        assertThat(normalizer.canonicalTitle("Track (Acoustic)"))
                .isEqualTo("Track (Acoustic)");
    }
}
