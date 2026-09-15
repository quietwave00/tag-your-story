package com.tagnote.application.enrichment.matching;

import com.tagnote.application.enrichment.matching.model.LastFmCatalogData.TopTags;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LastFmEntityMatchingService {

    private final MusicEntityNameNormalizer normalizer;

    public boolean matches(String expectedArtist, String expectedTitle, TopTags response) {
        return response != null
                && normalizer.exact(expectedArtist, response.artistName())
                && normalizer.exact(expectedTitle, response.title());
    }
}
