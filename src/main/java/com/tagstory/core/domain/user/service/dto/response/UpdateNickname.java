package com.tagstory.core.domain.user.service.dto.response;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class UpdateNickname {
    private String nickname;

    public static UpdateNickname onComplete(String nickname) {
        return UpdateNickname.builder()
                .nickname(nickname)
                .build();
    }
}
