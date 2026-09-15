package com.tagnote.application.catalog.search.model;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CatalogSearchItem {

    private CatalogSearchSubjectType subjectType;
    private String spotifyId;
    private String title;
    private String artistName;
    private String albumName;
    private String imageUrl;
    private int providerRank;

    public static CatalogSearchItem of(
            CatalogSearchSubjectType subjectType,
            String spotifyId,
            String title,
            String artistName,
            String albumName,
            String imageUrl,
            int providerRank
    ) {
        return CatalogSearchItem.builder()
                .subjectType(subjectType)
                .spotifyId(spotifyId)
                .title(title)
                .artistName(artistName)
                .albumName(albumName)
                .imageUrl(imageUrl)
                .providerRank(providerRank)
                .build();
    }
}
