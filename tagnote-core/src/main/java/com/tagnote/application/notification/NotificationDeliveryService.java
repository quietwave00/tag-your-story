package com.tagnote.application.notification;

import com.tagnote.core.domain.notification.service.Notification;
import com.tagnote.core.domain.notification.service.NotificationService;
import com.tagnote.core.domain.user.UserStatus;
import com.tagnote.core.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationDeliveryService {
    private final NotificationService notificationService;
    private final NotificationDeliveryPort deliveryPort;
    private final UserRepository users;

    public void deliver(Notification notification) {
        Notification saved;
        try {
            saved = notificationService.save(notification);
        } catch (RuntimeException e) {
            log.error("Notification save failed for subscriber {}", notification.getSubscriber().getUserId(), e);
            return;
        }
        if (saved == null) {
            return;
        }
        try {
            Long subscriberId = saved.getSubscriber().getUserId();
            if (users.findByUserId(subscriberId)
                    .map(user -> user.getUserStatus() == UserStatus.ACTIVE)
                    .orElse(false)) {
                deliveryPort.deliver(NotificationDelivery.from(saved));
            }
        } catch (RuntimeException e) {
            log.warn("Notification {} saved but live delivery failed", saved.getNotificationId(), e);
        }
    }
}
