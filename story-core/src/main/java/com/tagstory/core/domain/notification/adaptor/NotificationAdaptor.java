package com.tagstory.core.domain.notification.adaptor;

import com.tagstory.core.domain.event.CommonEvent;
import com.tagstory.core.domain.notification.service.Notification;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class NotificationAdaptor implements CommonEvent {

    private final Notification notification;

    public static NotificationAdaptor of(Notification notification) {
        return new NotificationAdaptor(notification);
    }
}
