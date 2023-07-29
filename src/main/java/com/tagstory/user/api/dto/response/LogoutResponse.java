package com.tagstory.user.api.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Builder
@Getter
public class LogoutResponse {
    private LocalDateTime logoutAt;
    private Long userId;
}
