package com.tagstory.api.domain.notification.dto.response;

import com.tagstory.core.domain.notification.service.Notification;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class NotificationResponse {
    private String contentId;
    private String pubNickname;

    public static NotificationResponse from(Notification notification) {
        return NotificationResponse.builder()
                .contentId(notification.getContentId())
                .pubNickname(notification.getPublisher().getNickname())
                .build();
    }
}
