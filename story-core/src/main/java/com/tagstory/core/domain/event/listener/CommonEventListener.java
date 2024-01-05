package com.tagstory.core.domain.event.listener;


import com.tagstory.core.domain.event.CommonEvent;
import com.tagstory.core.domain.notification.adaptor.NotificationAdaptor;
import com.tagstory.core.domain.notification.service.Notification;
import com.tagstory.core.domain.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
@Slf4j
public class CommonEventListener {

    private final NotificationService notificationService;

    @EventListener
    public void handleEvent(CommonEvent commonEvent) {
        if(commonEvent instanceof NotificationAdaptor) {
            Notification notification = commonEvent.getNotification();
            Notification savedNotification = notificationService.save(notification);
            sendNotification(savedNotification);
        }
    }

    public void sendNotification(Notification notification) {
        notificationService.send(notification);
    }
}
