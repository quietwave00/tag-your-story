package com.tagstory.api.domain.user.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Builder
@Getter
public class LogoutResponse {
    private LocalDateTime logoutAt;
    private Long userId;

    public static LogoutResponse create(com.tagstory.core.domain.user.service.dto.response.Logout logoutResponse) {
        return LogoutResponse.builder()
                .logoutAt(logoutResponse.getLogoutAt())
                .userId(logoutResponse.getUserId())
                .build();
    }
}
