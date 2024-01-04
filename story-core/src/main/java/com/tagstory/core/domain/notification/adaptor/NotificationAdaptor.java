package com.tagstory.core.domain.notification.adaptor;

import com.tagstory.core.domain.event.CommonEvent;
import com.tagstory.core.domain.notification.NotificationType;
import com.tagstory.core.domain.notification.service.Notification;
import com.tagstory.core.domain.user.service.User;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class NotificationAdaptor implements CommonEvent {
    private final Notification notification;

    @Override
    public User getPublisher() {
        return notification.getPublisher();
    }

    @Override
    public User getSubscriber() {
        return notification.getSubscriber();
    }

    @Override
    public NotificationType getType() {
        return notification.getType();
    }

    @Override
    public String getContentId() {
        return notification.getContentId();
    }

    public static NotificationAdaptor from(CommonEvent commonEvent) {
        Notification notification = Notification.builder()
                .publisher(commonEvent.getPublisher())
                .subscriber(commonEvent.getSubscriber())
                .build();
    }
}
