package com.tagstory.core.domain.boardhashtag.service.dto;

import com.tagstory.core.domain.hashtag.HashtagEntity;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.stream.Collectors;

@Getter
@Builder
public class HashtagNameList {
    private List<String> nameList;

    public static HashtagNameList onComplete(List<String> nameList) {
        return HashtagNameList.builder()
                .nameList(nameList)
                .build();
    }

    public static HashtagNameList of(List<HashtagEntity> hashtagEntityList) {
        return HashtagNameList.builder()
                .nameList(hashtagEntityList.stream().map(HashtagEntity::getName).collect(Collectors.toList()))
                .build();
    }
}
