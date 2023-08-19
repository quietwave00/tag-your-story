package com.tagstory.core.domain.user.service.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Builder
@Getter
public class UpdateNicknameResponse {
    private LocalDateTime updateAt;
    private String nickname;

    public static UpdateNicknameResponse onComplete(String nickname) {
        return UpdateNicknameResponse.builder()
                .updateAt(LocalDateTime.now())
                .nickname(nickname)
                .build();
    }
}
