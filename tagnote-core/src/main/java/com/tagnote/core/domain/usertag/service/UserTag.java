package com.tagnote.core.domain.usertag.service;

import com.tagnote.core.domain.usertag.UserTagEntity;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserTag {
    private Long userTagId;

    private String name;

    /*
     * 형변환
     */
    public UserTagEntity toEntity() {
        return UserTagEntity.builder()
                .userTagId(this.getUserTagId())
                .name(this.getName())
                .build();
    }
}
