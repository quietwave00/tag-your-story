package com.tagnote.application.notification;

import com.tagnote.core.domain.notification.NotificationType;
import com.tagnote.core.domain.notification.service.Notification;

public record NotificationDelivery(Long notificationId, Long subscriberId, String contentId,
                                   String pubNickname, NotificationType type, boolean isRead) {
    public static NotificationDelivery from(Notification notification) {
        return new NotificationDelivery(notification.getNotificationId(),
                notification.getSubscriber().getUserId(), notification.getContentId(),
                notification.getPublisher().getDisplayNickname(), notification.getType(),
                Boolean.TRUE.equals(notification.getIsRead()));
    }
}
