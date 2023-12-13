package com.tagstory.core.domain.notification.listener;

import com.tagstory.core.domain.notification.service.Notification;
import com.tagstory.core.domain.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@RequiredArgsConstructor
@Component
@Slf4j
public class NotificationListener {

    private final NotificationService notificationService;

    @TransactionalEventListener
    public void handleNotification(Notification notification) {
        Notification savedNotification = notificationService.save(notification);
        sendNotification(savedNotification);
    }

    public void sendNotification(Notification notification) {
        notificationService.send(notification);
    }
}
