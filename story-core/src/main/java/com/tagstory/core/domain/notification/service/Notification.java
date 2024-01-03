package com.tagstory.core.domain.notification.service;

import com.tagstory.core.domain.event.Event;
import com.tagstory.core.domain.notification.NotificationEntity;
import com.tagstory.core.domain.notification.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Notification extends Event {

    private Long notificationId;

    private NotificationType type;

    private String contentId;

    private Boolean isRead;

    /*
     * 비즈니스 로직
     */
    public static NotificationEntity create(Notification notification) {
        return NotificationEntity.builder()
                .notificationId(notification.getNotificationId())
                .publisher(notification.getPublisher().toEntity())
                .subscriber(notification.getSubscriber().toEntity())
                .type(notification.getType())
                .contentId(notification.getContentId())
                .isRead(false)
                .build();
    }

    /*
     * 형변환
     */
    public NotificationEntity toEntity() {
        return NotificationEntity.builder()
                .notificationId(this.notificationId)
                .publisher(super.getPublisher().toEntity())
                .subscriber(super.getSubscriber().toEntity())
                .type(this.type)
                .contentId(this.contentId)
                .isRead(this.isRead)
                .build();
    }
}
