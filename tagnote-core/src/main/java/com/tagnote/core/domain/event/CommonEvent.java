package com.tagnote.core.domain.event;

import com.tagnote.core.domain.notification.service.Notification;

public interface CommonEvent {

    Notification getNotification();
}
