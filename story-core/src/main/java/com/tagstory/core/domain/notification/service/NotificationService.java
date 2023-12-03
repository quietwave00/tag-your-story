package com.tagstory.core.domain.notification.service;

import com.tagstory.core.domain.notification.NotificationType;
import com.tagstory.core.domain.notification.service.dto.command.NotificationCommand;
import com.tagstory.core.domain.notification.sse.SseManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationManager notificationManager;
    private final SseManager sseFactory;

    public void notifyComment(Long userId, String nickname, String contentId) {
        NotificationCommand command = NotificationCommand.of(userId, nickname, NotificationType.COMMENT, contentId);
        notificationManager.sendMessage(command);
    }

    public SseEmitter subscribe(Long userId, String lastEventId, LocalDateTime createdAt) {
        return sseFactory.create(userId, createdAt);
    }
}
