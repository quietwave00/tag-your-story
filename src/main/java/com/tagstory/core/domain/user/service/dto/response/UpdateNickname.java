package com.tagstory.core.domain.user.service.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Builder
@Getter
public class UpdateNickname {
    private LocalDateTime updateAt;
    private String nickname;

    public static UpdateNickname onComplete(String nickname) {
        return UpdateNickname.builder()
                .updateAt(LocalDateTime.now())
                .nickname(nickname)
                .build();
    }
}
