package com.tagstory.api.domain.user.dto.response;

import com.tagstory.core.domain.user.service.dto.response.CheckRegisterUser;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CheckRegisterUserResponse {
    private boolean isRegisterUser;

    public static CheckRegisterUserResponse from(CheckRegisterUser checkRegisterUserResponse) {
        return CheckRegisterUserResponse.builder()
                .isRegisterUser(checkRegisterUserResponse.isRegisterUser())
                .build();
    }
}
