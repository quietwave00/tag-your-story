package com.tagnote.application.catalog.search;

import com.tagnote.application.catalog.search.model.CatalogSearchItem;
import com.tagnote.application.catalog.search.model.CatalogSearchResult;
import com.tagnote.application.catalog.search.model.CatalogSearchScope;
import com.tagnote.application.catalog.search.model.CatalogSearchSubjectType;
import com.tagnote.application.catalog.search.port.CatalogSearchProvider;
import com.tagnote.application.catalog.search.port.SearchKeywordRecorder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CatalogSearchServiceTest {

    @Mock
    private SearchKeywordRecorder searchKeywordRecorder;

    @Mock
    private CatalogSearchProvider catalogSearchProvider;

    @InjectMocks
    private CatalogSearchService service;

    @Test
    void 통합_검색은_검색어를_한번_기록한_뒤_제공자를_호출하고_전체_건수를_유지한다() {
        when(catalogSearchProvider.search("Queen", 2, CatalogSearchScope.ALL)).thenReturn(CatalogSearchResult.of(List.of(), 125L));

        CatalogSearchResult result = service.search("Queen", 2);

        InOrder inOrder = inOrder(searchKeywordRecorder, catalogSearchProvider);
        inOrder.verify(searchKeywordRecorder).record("Queen");
        inOrder.verify(catalogSearchProvider).search("Queen", 2, CatalogSearchScope.ALL);
        assertThat(result.getTotalCount()).isEqualTo(125L);
    }

    @Test
    void 통합_검색은_정확한_이름과_접두어와_이름_유사도와_가수_유사도_순으로_정렬한다() {
        CatalogSearchItem artistSimilarity = item(
                CatalogSearchSubjectType.TRACK, "4", "Unrelated", "Queen", 0
        );
        CatalogSearchItem stringSimilarity = item(
                CatalogSearchSubjectType.ALBUM, "3", "Quean", "Someone", 0
        );
        CatalogSearchItem prefix = item(
                CatalogSearchSubjectType.TRACK, "2", "Queen II", "Queen", 1
        );
        CatalogSearchItem exact = item(
                CatalogSearchSubjectType.ARTIST, "1", "  QUEEN  ", null, 0
        );
        when(catalogSearchProvider.search("Queen", 0, CatalogSearchScope.ALL)).thenReturn(CatalogSearchResult.of(
                List.of(artistSimilarity, stringSimilarity, prefix, exact),
                4L
        ));

        CatalogSearchResult result = service.search("Queen", 0);

        assertThat(result.getItems()).extracting(CatalogSearchItem::getSpotifyId)
                .containsExactly("1", "2", "3", "4");
    }

    @Test
    void 통합_검색은_동점이면_원본_순서와_대상_종류로_정렬한다() {
        CatalogSearchItem albumRankOne = item(CatalogSearchSubjectType.ALBUM, "album-1", "same", null, 1);
        CatalogSearchItem artistRankZero = item(CatalogSearchSubjectType.ARTIST, "artist-0", "same", null, 0);
        CatalogSearchItem trackRankZero = item(CatalogSearchSubjectType.TRACK, "track-0", "same", null, 0);
        when(catalogSearchProvider.search("same", 0, CatalogSearchScope.ALL)).thenReturn(CatalogSearchResult.of(
                List.of(albumRankOne, artistRankZero, trackRankZero),
                3L
        ));

        CatalogSearchResult result = service.search("same", 0);

        assertThat(result.getItems()).extracting(CatalogSearchItem::getSpotifyId)
                .containsExactly("track-0", "artist-0", "album-1");
    }

    @Test
    void 타입별_검색은_선택한_범위를_전달하고_해당_타입의_전체_건수를_유지한다() {
        when(catalogSearchProvider.search("Queen", 3, CatalogSearchScope.ARTIST))
                .thenReturn(CatalogSearchResult.of(List.of(
                        item(CatalogSearchSubjectType.ARTIST, "artist-2", "Queen II", null, 0),
                        item(CatalogSearchSubjectType.ARTIST, "artist-1", "Queen", null, 1)), 87L));

        CatalogSearchResult result = service.search("Queen", 3, CatalogSearchScope.ARTIST);

        InOrder order = inOrder(searchKeywordRecorder, catalogSearchProvider);
        order.verify(searchKeywordRecorder).record("Queen");
        order.verify(catalogSearchProvider).search("Queen", 3, CatalogSearchScope.ARTIST);
        assertThat(result.getItems()).extracting(CatalogSearchItem::getSpotifyId)
                .containsExactly("artist-1", "artist-2");
        assertThat(result.getTotalCount()).isEqualTo(87L);
    }

    private CatalogSearchItem item(
            CatalogSearchSubjectType subjectType,
            String spotifyId,
            String title,
            String artistName,
            int providerRank
    ) {
        return CatalogSearchItem.of(
                subjectType,
                spotifyId,
                title,
                artistName,
                null,
                "image",
                providerRank
        );
    }
}
