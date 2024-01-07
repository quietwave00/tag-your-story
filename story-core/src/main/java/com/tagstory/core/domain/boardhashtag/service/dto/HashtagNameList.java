package com.tagstory.core.domain.boardhashtag.service.dto;

import com.tagstory.core.domain.hashtag.HashtagEntity;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class HashtagNameList {
    private List<String> nameList;

    public static HashtagNameList onComplete(List<String> nameList) {
        return builder()
                .nameList(nameList)
                .build();
    }

    public static HashtagNameList of(List<HashtagEntity> hashtagEntityList) {
        return builder()
                .nameList(hashtagEntityList.stream().map(HashtagEntity::getName).toList())
                .build();
    }
}
