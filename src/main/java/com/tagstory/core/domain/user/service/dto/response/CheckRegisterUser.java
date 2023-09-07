package com.tagstory.core.domain.user.service.dto.response;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class CheckRegisterUser {
    private boolean isRegisterUser;

    public static CheckRegisterUser onComplete(boolean status) {
        return CheckRegisterUser.builder()
                .isRegisterUser(status)
                .build();
    }
}
