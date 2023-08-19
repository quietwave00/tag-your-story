package com.tagstory.core.domain.user.service.dto.response;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class CheckRegisterUserResponse {
    private boolean isRegisterUser;

    public static CheckRegisterUserResponse onComplete(boolean status) {
        return CheckRegisterUserResponse.builder()
                .isRegisterUser(status)
                .build();
    }
}
