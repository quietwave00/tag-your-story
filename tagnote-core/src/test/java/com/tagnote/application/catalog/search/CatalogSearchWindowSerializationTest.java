package com.tagnote.application.catalog.search;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tagnote.application.catalog.search.model.CatalogSearchItem;
import com.tagnote.application.catalog.search.model.CatalogSearchSubjectType;
import com.tagnote.application.catalog.search.model.CatalogSearchWindow;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CatalogSearchWindowSerializationTest {
    @Test
    void 검색_window를_Redis_JSON으로_저장하고_다시_읽을_수_있다() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        CatalogSearchWindow original = new CatalogSearchWindow(List.of(
                CatalogSearchItem.of(CatalogSearchSubjectType.TRACK, "track-1", "Song",
                        "Band", "Album", "image", 20)), List.of(), List.of(), 31, 12, 4);

        CatalogSearchWindow restored = mapper.readValue(mapper.writeValueAsString(original),
                CatalogSearchWindow.class);

        assertThat(restored.tracks()).hasSize(1);
        assertThat(restored.tracks().get(0).getSpotifyId()).isEqualTo("track-1");
        assertThat(restored.tracks().get(0).getProviderRank()).isEqualTo(20);
        assertThat(restored.trackTotalCount()).isEqualTo(31);
        assertThat(restored.fetchedTrackCount()).isEqualTo(1);
    }
}
