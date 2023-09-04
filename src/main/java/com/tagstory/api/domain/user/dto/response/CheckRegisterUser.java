package com.tagstory.api.domain.user.dto.response;

import com.tagstory.core.domain.user.service.dto.response.CheckRegisterUserResponse;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CheckRegisterUser {
    private boolean isRegisterUser;

    public static CheckRegisterUser create(CheckRegisterUserResponse checkRegisterUserResponse) {
        return CheckRegisterUser.builder()
                .isRegisterUser(checkRegisterUserResponse.isRegisterUser())
                .build();
    }
}
