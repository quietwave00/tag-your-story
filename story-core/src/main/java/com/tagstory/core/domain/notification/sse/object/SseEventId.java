package com.tagstory.core.domain.notification.sse.object;

import lombok.Builder;

@Builder
public class SseEventId {

    private String key;
    private String eventName;

    private static final String SEPARATOR = "_";

    public static String generate(String eventName, String key) {
        return eventName + SEPARATOR + key;
    }
}
