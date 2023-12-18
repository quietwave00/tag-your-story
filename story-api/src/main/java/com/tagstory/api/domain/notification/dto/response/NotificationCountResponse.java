package com.tagstory.api.domain.notification.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class NotificationCountResponse {
    int count;

    public static NotificationCountResponse from(int count) {
        return builder()
                .count(count)
                .build();
    }
}
