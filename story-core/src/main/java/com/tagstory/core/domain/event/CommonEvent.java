package com.tagstory.core.domain.event;

import com.tagstory.core.domain.notification.service.Notification;

public interface CommonEvent {

    Notification getNotification();
}
