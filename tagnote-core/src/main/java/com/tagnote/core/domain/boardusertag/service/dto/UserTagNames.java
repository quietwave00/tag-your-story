package com.tagnote.core.domain.boardusertag.service.dto;

import com.tagnote.core.domain.usertag.UserTagEntity;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class UserTagNames {
    private List<String> nameList;

    public static UserTagNames ofNameList(List<String> nameList) {
        return builder()
                .nameList(nameList)
                .build();
    }

    public static UserTagNames ofEntityList(List<UserTagEntity> userTagEntityList) {
        return builder()
                .nameList(userTagEntityList.stream().map(UserTagEntity::getName).toList())
                .build();
    }
}
