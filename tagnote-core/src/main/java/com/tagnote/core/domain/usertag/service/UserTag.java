package com.tagnote.core.domain.usertag.service;

import com.tagnote.core.domain.usertag.UserTagEntity;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserTag {
    private Long userTagId;

    private Long ownerUserId;

    private String name;

    private String normalizedName;

    /*
     * 형변환
     */
    public UserTagEntity toEntity() {
        return UserTagEntity.builder()
                .userTagId(this.getUserTagId())
                .owner(com.tagnote.core.domain.user.UserEntity.builder().userId(this.getOwnerUserId()).build())
                .name(this.getName())
                .normalizedName(this.getNormalizedName())
                .build();
    }
}
