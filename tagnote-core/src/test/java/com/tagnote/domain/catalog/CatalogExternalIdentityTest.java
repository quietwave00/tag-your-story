package com.tagnote.domain.catalog;

import com.tagnote.domain.catalog.album.AlbumEntity;
import com.tagnote.domain.catalog.track.TrackEntity;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CatalogExternalIdentityTest {

    @Test
    void MusicBrainz_ID는_null에서만_설정하고_동일값은_멱등하다() {
        AlbumEntity album = AlbumEntity.create("Album", "album", 2026);
        TrackEntity track = TrackEntity.create("Track", "track", "ISRC", 180_000, album);

        track.attachMusicBrainzRecordingId("recording-1");
        track.attachMusicBrainzRecordingId("recording-1");

        assertThat(track.getMusicbrainzId()).isEqualTo("recording-1");
        assertThatThrownBy(() -> track.attachMusicBrainzRecordingId("recording-2"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("identity conflict");
    }
}
