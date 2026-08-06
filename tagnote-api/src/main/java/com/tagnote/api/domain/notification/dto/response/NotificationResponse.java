package com.tagnote.api.domain.notification.dto.response;

import com.tagnote.core.domain.notification.NotificationType;
import com.tagnote.core.domain.notification.service.Notification;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class NotificationResponse {
    private Long notificationId;
    private String contentId;
    private String pubNickname;
    private NotificationType type;
    private boolean isRead;

    public static NotificationResponse from(Notification notification) {
        return NotificationResponse.builder()
                .notificationId(notification.getNotificationId())
                .contentId(notification.getContentId())
                .pubNickname(notification.getPublisher().getNickname())
                .type(notification.getType())
                .isRead(notification.getIsRead())
                .build();
    }
}
