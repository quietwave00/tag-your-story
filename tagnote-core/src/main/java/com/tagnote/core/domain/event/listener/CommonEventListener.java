package com.tagnote.core.domain.event.listener;


import com.tagnote.core.domain.event.CommonEvent;
import com.tagnote.core.domain.notification.adaptor.NotificationAdaptor;
import com.tagnote.application.notification.NotificationDeliveryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@RequiredArgsConstructor
@Component
@Slf4j
public class CommonEventListener {

    private final NotificationDeliveryService deliveryService;

    @TransactionalEventListener
    @Async
    public void handleEvent(CommonEvent commonEvent) {
        log.info("Event Listen");
        if(commonEvent instanceof NotificationAdaptor) {
            deliveryService.deliver(commonEvent.getNotification());
        }
    }

}
