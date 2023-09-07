package com.tagstory.core.domain.user.service.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Builder
@Getter
public class Logout {
    private LocalDateTime logoutAt;
    private Long userId;

    public static Logout onComplete(Long userId) {
        return Logout.builder()
                .logoutAt(LocalDateTime.now())
                .userId(userId)
                .build();
    }
}
