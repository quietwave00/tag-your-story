package com.tagstory.core.domain.tracks.service.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class RankingList {
    private List<String> keywordList;

    public static RankingList onComplete(List<String> keywordList) {
        return builder()
                .keywordList(keywordList)
                .build();
    }
}
