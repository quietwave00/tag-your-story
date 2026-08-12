package com.tagnote.application.catalog.search;

import com.tagnote.application.catalog.search.model.TrackSearchItem;
import com.tagnote.application.catalog.search.model.TrackSearchResult;
import com.tagnote.application.catalog.search.port.SearchKeywordRecorder;
import com.tagnote.application.catalog.search.port.TrackSearchProvider;
import com.tagnote.core.exception.CustomException;
import com.tagnote.core.exception.ExceptionCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrackSearchServiceTest {

    @Mock
    private SearchKeywordRecorder searchKeywordRecorder;

    @Mock
    private TrackSearchProvider trackSearchProvider;

    @InjectMocks
    private TrackSearchService trackSearchService;

    @Test
    void search는_keyword를_기록한_후_provider_결과를_그대로_반환한다() {
        TrackSearchResult expected = TrackSearchResult.of(
                List.of(
                        TrackSearchItem.of("track-1", "artist-1", "title-1", "album-1", "image-1"),
                        TrackSearchItem.of("track-2", "artist-2", "title-2", "album-2", "image-2")
                ),
                25
        );
        when(trackSearchProvider.search("rock", 2)).thenReturn(expected);

        TrackSearchResult result = trackSearchService.search("rock", 2);

        InOrder inOrder = inOrder(searchKeywordRecorder, trackSearchProvider);
        inOrder.verify(searchKeywordRecorder).record("rock");
        inOrder.verify(trackSearchProvider).search("rock", 2);
        assertThat(result).isSameAs(expected);
        assertThat(result.getItems()).extracting(TrackSearchItem::getSpotifyTrackId)
                .containsExactly("track-1", "track-2");
        assertThat(result.getTotalCount()).isEqualTo(25);
    }

    @Test
    void keyword_기록이_실패하면_provider를_호출하지_않는다() {
        CustomException exception = new CustomException(ExceptionCode.SPOTIFY_EXCEPTION);
        org.mockito.Mockito.doThrow(exception).when(searchKeywordRecorder).record("rock");

        assertThatThrownBy(() -> trackSearchService.search("rock", 0))
                .isSameAs(exception);
        verify(trackSearchProvider, never()).search("rock", 0);
    }

    @Test
    void provider가_실패하면_기록을_완료한_뒤_같은_예외를_전파한다() {
        CustomException exception = new CustomException(ExceptionCode.SPOTIFY_EXCEPTION);
        when(trackSearchProvider.search("rock", 0)).thenThrow(exception);

        assertThatThrownBy(() -> trackSearchService.search("rock", 0))
                .isSameAs(exception);

        InOrder inOrder = inOrder(searchKeywordRecorder, trackSearchProvider);
        inOrder.verify(searchKeywordRecorder).record("rock");
        inOrder.verify(trackSearchProvider).search("rock", 0);
    }
}
