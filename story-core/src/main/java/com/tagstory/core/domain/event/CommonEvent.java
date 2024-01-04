package com.tagstory.core.domain.event;

import com.tagstory.core.domain.notification.NotificationType;
import com.tagstory.core.domain.user.service.User;

public interface CommonEvent {
    User getPublisher();

    User getSubscriber();

    NotificationType getType();

    String getContentId();
}
