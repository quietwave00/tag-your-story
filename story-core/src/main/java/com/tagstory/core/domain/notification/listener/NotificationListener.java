package com.tagstory.core.domain.notification.listener;

import com.tagstory.core.domain.notification.service.Notification;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class NotificationListener {
    @EventListener
    public void handleNotification(Notification notification) {
        System.out.println("Received notification: " + notification.getPublisher().getNickname());
    }
}
