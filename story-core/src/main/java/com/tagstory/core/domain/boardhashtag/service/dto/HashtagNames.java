package com.tagstory.core.domain.boardhashtag.service.dto;

import com.tagstory.core.domain.hashtag.HashtagEntity;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class HashtagNames {
    private List<String> nameList;

    public static HashtagNames ofNameList(List<String> nameList) {
        return builder()
                .nameList(nameList)
                .build();
    }

    public static HashtagNames of(List<HashtagEntity> hashtagEntityList) {
        return builder()
                .nameList(hashtagEntityList.stream().map(HashtagEntity::getName).toList())
                .build();
    }
}
