package com.tagstory.core.domain.notification.sse.object;

import java.time.LocalDateTime;

public class SseKey {

    private Long userId;
    private LocalDateTime createdAt;

    private static final String SEPARATOR = "";

    public static String generate(Long userId, LocalDateTime createdAt) {
        return userId + SEPARATOR +  createdAt.toString();
    }
}
