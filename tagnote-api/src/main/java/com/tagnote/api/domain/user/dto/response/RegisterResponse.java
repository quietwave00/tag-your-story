package com.tagnote.api.domain.user.dto.response;

import com.tagnote.core.domain.user.service.User;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class RegisterResponse {
    private String nickname;

    public static RegisterResponse from(User user) {
        return builder()
                .nickname(user.getNickname())
                .build();
    }
}
