package com.tagnote.application.catalog.detail;

import com.tagnote.application.catalog.detail.model.ArtistAlbumIndexPage;
import com.tagnote.application.catalog.detail.model.CatalogReleaseType;
import com.tagnote.application.catalog.detail.model.SpotifyAlbumSummary;
import com.tagnote.application.catalog.detail.model.SpotifyCatalogPage;
import com.tagnote.application.catalog.detail.port.SpotifyCatalogDetailProvider;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class CatalogDetailServiceTest {
    private final SpotifyCatalogDetailProvider provider = mock(SpotifyCatalogDetailProvider.class);
    private final ArtistAlbumIndexService indexService = mock(ArtistAlbumIndexService.class);
    private final CatalogDetailService service = new CatalogDetailService(provider, indexService);

    @Test
    void 유형이_없으면_Spotify_원본_페이지와_건수를_사용한다() {
        when(provider.getArtistAlbums("artist", 2)).thenReturn(new SpotifyCatalogPage<>(
                List.of(new SpotifyAlbumSummary("album", "Album", "cover", null, CatalogReleaseType.ALBUM)), 31));

        com.tagnote.application.catalog.detail.model.ArtistAlbumListing listing = service.getArtistAlbumListing("artist", null, 2).orElseThrow();

        assertThat(listing.totalCount()).isEqualTo(31);
        assertThat(listing.classificationComplete()).isNull();
        verifyNoInteractions(indexService);
    }

    @Test
    void 유형이_있으면_분류된_인덱스_페이지만_사용한다() {
        when(indexService.read("artist", "ALBUM", 1)).thenReturn(Optional.of(new ArtistAlbumIndexPage(
                List.of(new SpotifyAlbumSummary("album", "Album", "cover", null, CatalogReleaseType.ALBUM)),
                11, false, 2)));

        com.tagnote.application.catalog.detail.model.ArtistAlbumListing listing = service.getArtistAlbumListing("artist", "ALBUM", 1).orElseThrow();

        assertThat(listing.totalCount()).isEqualTo(11);
        assertThat(listing.classificationComplete()).isFalse();
        assertThat(listing.unclassifiedCount()).isEqualTo(2);
        verifyNoInteractions(provider);
    }
}
