package com.tagnote.application.catalog.detail.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CatalogReleaseTypeTest {
    @Test
    void 컴필레이션은_곡수와_재생시간보다_우선한다() {
        assertThat(CatalogReleaseType.classify("compilation", 2, 500_000L))
                .isEqualTo(CatalogReleaseType.COMPILATION);
    }

    @Test
    void Spotify_싱글은_싱글_EP로_묶는다() {
        assertThat(CatalogReleaseType.classify("single", 1, null))
                .isEqualTo(CatalogReleaseType.SINGLE_EP);
    }

    @Test
    void 여섯_곡_이하는_재생시간과_관계없이_싱글_EP다() {
        assertThat(CatalogReleaseType.classify("album", 6, null))
                .isEqualTo(CatalogReleaseType.SINGLE_EP);
    }

    @Test
    void 일곱_곡_이상도_삼십_분_미만이면_싱글_EP다() {
        assertThat(CatalogReleaseType.classify("album", 7, 1_799_999L))
                .isEqualTo(CatalogReleaseType.SINGLE_EP);
    }

    @Test
    void 일곱_곡_이상에_정확히_삼십_분이면_앨범이다() {
        assertThat(CatalogReleaseType.classify("album", 7, 1_800_000L))
                .isEqualTo(CatalogReleaseType.ALBUM);
    }

    @Test
    void 분류에_필요한_Spotify_정보가_없으면_추측하지_않는다() {
        assertThat(CatalogReleaseType.classify("album", 7, null)).isNull();
        assertThat(CatalogReleaseType.classify(null, 7, 1_800_000L)).isNull();
    }
}
