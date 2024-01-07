package com.tagstory.api.domain.user.dto.response;

import com.tagstory.core.domain.user.service.User;
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
