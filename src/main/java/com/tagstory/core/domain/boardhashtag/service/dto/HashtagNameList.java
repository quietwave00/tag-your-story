package com.tagstory.core.domain.boardhashtag.service.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class HashtagNameList {
    private List<String> nameList;

    public static HashtagNameList onComplete(List<String> nameList) {
        return HashtagNameList.builder()
                .nameList(nameList)
                .build();
    }
}
