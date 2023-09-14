package com.tagstory.api.domain.user.dto.response;

import com.tagstory.core.domain.user.service.dto.response.Logout;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Builder
@Getter
public class LogoutResponse {
    private LocalDateTime logoutAt;
    private Long userId;

    public static LogoutResponse from(Logout logoutResponse) {
        return LogoutResponse.builder()
                .logoutAt(logoutResponse.getLogoutAt())
                .userId(logoutResponse.getUserId())
                .build();
    }
}
