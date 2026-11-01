package com.tagnote.domain.tracks.service;

import com.tagnote.application.catalog.detail.port.TrackDetailProvider;
import com.tagnote.application.catalog.search.port.SearchKeywordRankingReader;
import com.tagnote.core.domain.tracks.service.TrackService;
import com.tagnote.core.domain.tracks.service.dto.TrackData;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class TrackServiceTest {
    private final TrackDetailProvider detail = mock(TrackDetailProvider.class);
    private final SearchKeywordRankingReader ranking = mock(SearchKeywordRankingReader.class);
    private final TrackService service = new TrackService(detail, ranking);

    @Test
    void detail_delegates_to_provider() {
        var track = TrackData.of("t", "A", "Title", "Album", "NO_IMAGE", "a", "al");
        when(detail.getTrack("t")).thenReturn(track);
        assertThat(service.getDetail("t")).isSameAs(track);
        verify(detail).getTrack("t");
    }

    @Test
    void ranking_preserves_reader_order() {
        when(ranking.getTopSearchKeywordList()).thenReturn(List.of("rock", "pop"));
        assertThat(service.getKeywordRanking().getKeywordList()).containsExactly("rock", "pop");
    }
}
