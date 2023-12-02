package com.tagstory.core.domain.notification.service;

import com.tagstory.core.domain.notification.NotificationEntity;
import com.tagstory.core.domain.notification.NotificationType;
import com.tagstory.core.domain.user.service.dto.response.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

    private Long notificationId;

    private User publisher;

    private User subscriber;

    private NotificationType type;

    private String contentId;

    /*
     * 비즈니스 로직
     */
    public static Notification onEvent(User publisher, User subscriber, NotificationType type, String contentId) {
        return Notification.builder()
                .publisher(publisher)
                .subscriber(subscriber)
                .type(type)
                .contentId(contentId)
                .build();
    }

    /*
     * 형변환
     */
    public NotificationEntity toEntity() {
        return NotificationEntity.builder()
                .notificationId(this.notificationId)
                .publisher(this.publisher.toEntity())
                .subscriber(this.subscriber.toEntity())
                .type(this.type)
                .contentId(this.contentId)
                .build();
    }
}
