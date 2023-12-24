package com.tagstory.domain.notification.fixture;

import com.tagstory.core.domain.notification.NotificationEntity;
import com.tagstory.core.domain.notification.service.Notification;
import com.tagstory.domain.user.fixture.UserFixture;

public class NotificationFixture {
    public static Notification createNotification(Long notificationId) {
        return Notification.builder()
                .notificationId(notificationId)
                .subscriber(UserFixture.createUser(1L))
                .build();
    }

    public static NotificationEntity createNotificationEntity(Long notificationId) {
        return NotificationEntity.builder()
                .notificationId(notificationId)
                .subscriber(UserFixture.createUser(1L).toEntity())
                .build();
    }
}
