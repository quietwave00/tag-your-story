package com.tagstory.core.domain.notification.service;

import com.tagstory.core.domain.notification.NotificationType;
import com.tagstory.core.domain.notification.service.dto.command.NotificationCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationManager notificationManager;

    public void notifyComment(Long userId, String nickname, String contentId) {
        NotificationCommand command = NotificationCommand.of(userId, nickname, NotificationType.COMMENT, contentId);
        notificationManager.sendMessage(command);
    }

}
