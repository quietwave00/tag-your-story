package com.tagstory.core.domain.notification.service;

import com.tagstory.core.domain.notification.dto.command.NotificationReadCommand;
import com.tagstory.core.domain.user.service.UserService;
import com.tagstory.core.domain.user.service.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@Component
@RequiredArgsConstructor
public class NotificationFacade {

    private final NotificationService notificationService;
    private final UserService userService;

    public SseEmitter subscribe(Long userId) {
        return notificationService.subscribe(userId);
    }

    public List<Notification> getNotificationList(Long userId, int page) {
        User user = userService.getCacheByUserId(userId);
        return notificationService.getNotificationList(user, page);
    }

    public void setAsRead(NotificationReadCommand command) {
        notificationService.setAsRead(command);
    }

    public int getNotificationCount(Long userId) {
        User user = userService.getCacheByUserId(userId);
        return notificationService.getNotificationCount(user);
    }

    public void setAllAsRead(Long userId) {
        User user = userService.getCacheByUserId(userId);
        notificationService.setAllAsRead(user);
    }
}
