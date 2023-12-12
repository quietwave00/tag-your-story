package com.tagstory.core.domain.notification.service;

import com.tagstory.core.domain.user.service.UserService;
import com.tagstory.core.domain.user.service.dto.response.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class NotificationFacade {

    private final NotificationService notificationService;
    private final UserService userService;

    public SseEmitter subscribe(Long userId, String lastEventId, LocalDateTime createdAt) {
        return notificationService.subscribe(userId, lastEventId, createdAt);
    }


    public List<Notification> getNotificationList(Long userId, int page) {
        User user = userService.getCacheByUserId(userId);
        return notificationService.getNotificationList(user, page);
    }
}