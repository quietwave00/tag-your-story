package com.tagstory.core.domain.user.service.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Builder
@Getter
public class LogoutResponse {
    private LocalDateTime logoutAt;
    private Long userId;

    public static LogoutResponse onComplete(Long userId) {
        return LogoutResponse.builder()
                .logoutAt(LocalDateTime.now())
                .userId(userId)
                .build();
    }
}
