package com.tagstory.core.domain.notification.listener;


import com.tagstory.core.domain.notification.service.Notification;
import com.tagstory.core.domain.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
@Slf4j
public class NotificationListener {

    private final NotificationService notificationService;

    @EventListener
    public void handleNotification(Notification notification) {
        Notification savedNotification = notificationService.save(notification);
        sendNotification(savedNotification);
    }

    public void sendNotification(Notification notification) {
        notificationService.send(notification);
    }
}
