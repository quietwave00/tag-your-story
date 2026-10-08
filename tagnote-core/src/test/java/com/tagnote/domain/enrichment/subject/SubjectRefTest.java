package com.tagnote.domain.enrichment.subject;

import com.tagnote.domain.catalog.album.AlbumEntity;
import com.tagnote.domain.catalog.track.TrackEntity;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SubjectRefTest {

    @Test
    void Track과_Album의_내부_PK를_타입과_함께_보존한다() {
        assertThat(SubjectRef.track(10L)).isEqualTo(new SubjectRef(SubjectType.TRACK, 10L));
        assertThat(SubjectRef.album(20L)).isEqualTo(new SubjectRef(SubjectType.ALBUM, 20L));
    }

    @Test
    void 이미_조회한_Catalog_Entity에서_생성한다() {
        AlbumEntity album = AlbumEntity.create("Album", "spotify-album", 2024);
        TrackEntity track = TrackEntity.create("Track", "spotify-track", "ISRC", 1000, album);
        ReflectionTestUtils.setField(album, "albumId", 20L);
        ReflectionTestUtils.setField(track, "trackId", 10L);

        assertThat(SubjectRef.track(track)).isEqualTo(SubjectRef.track(10L));
        assertThat(SubjectRef.album(album)).isEqualTo(SubjectRef.album(20L));
    }

    @Test
    void null이거나_양수가_아닌_ID를_거부한다() {
        assertThatThrownBy(() -> SubjectRef.track(0L)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> SubjectRef.album(-1L)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> SubjectRef.track(TrackEntity.create(
                "Track",
                "spotify-track",
                null,
                1000,
                AlbumEntity.create("Album", "spotify-album", null)
        ))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void MVP_SubjectType은_TRACK과_ALBUM뿐이다() {
        assertThat(Arrays.asList(SubjectType.values()))
                .containsExactly(SubjectType.TRACK, SubjectType.ALBUM);
    }
}
