package com.tagnote.infrastructure.external.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tagnote.application.catalog.detail.model.ArtistAlbumIndex;
import com.tagnote.application.catalog.detail.model.CatalogReleaseType;
import com.tagnote.application.catalog.detail.model.SpotifyAlbumSummary;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class RedisArtistAlbumIndexStoreTest {
    private final StringRedisTemplate redis = mock(StringRedisTemplate.class);
    @SuppressWarnings("unchecked")
    private final ValueOperations<String, String> values = mock(ValueOperations.class);
    private final RedisArtistAlbumIndexStore store = new RedisArtistAlbumIndexStore(redis, new ObjectMapper());

    @Test
    void 완전한_인덱스를_한_키에_원자적으로_저장하고_십오_분_유지한다() {
        when(redis.opsForValue()).thenReturn(values);
        ArtistAlbumIndex index = new ArtistAlbumIndex(
                List.of(new SpotifyAlbumSummary("album", "Album", "NO_IMAGE", null, CatalogReleaseType.ALBUM)),
                List.of(0), List.of(), List.of(), true, 0, 123L, 1);

        store.save("artist", index);

        verify(values).set(eq("artistAlbumIndex:classificationV1:formatV1:artist"), any(String.class),
                eq(Duration.ofMinutes(15)));
        verifyNoMoreInteractions(values);
    }

    @Test
    void 불완전한_인덱스는_일_분만_유지하고_다시_읽는다() throws Exception {
        when(redis.opsForValue()).thenReturn(values);
        ArtistAlbumIndex index = new ArtistAlbumIndex(
                List.of(new SpotifyAlbumSummary("unknown", "Unknown", "NO_IMAGE", null, null)),
                List.of(), List.of(), List.of(), false, 1, 123L, 1);
        when(values.get("artistAlbumIndex:classificationV1:formatV1:artist"))
                .thenReturn(new ObjectMapper().writeValueAsString(index));

        store.save("artist", index);
        ArtistAlbumIndex restored = store.find("artist");

        verify(values).set(eq("artistAlbumIndex:classificationV1:formatV1:artist"), any(String.class),
                eq(Duration.ofMinutes(1)));
        assertThat(restored.allItems()).extracting(SpotifyAlbumSummary::spotifyId).containsExactly("unknown");
        assertThat(restored.unclassifiedCount()).isEqualTo(1);
    }
}
