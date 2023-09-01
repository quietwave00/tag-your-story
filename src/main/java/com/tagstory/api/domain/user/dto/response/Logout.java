package com.tagstory.api.domain.user.dto.response;

import com.tagstory.core.domain.user.service.dto.response.LogoutResponse;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Builder
@Getter
public class Logout {
    private LocalDateTime logoutAt;
    private Long userId;

    public static Logout create(LogoutResponse logoutResponse) {
        return Logout.builder()
                .logoutAt(logoutResponse.getLogoutAt())
                .userId(logoutResponse.getUserId())
                .build();
    }
}
