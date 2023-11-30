package com.tagstory.api.domain.notification.dto.request;

import com.tagstory.core.domain.notification.NotificationType;
import com.tagstory.core.domain.notification.service.dto.command.NotificationCommand;
import lombok.Getter;

@Getter
public class NotificationRequest {
    private NotificationType type;
    private String contentId;

    public NotificationCommand toCommand(Long userId) {
        return NotificationCommand.builder()
                .userId(userId)
                .type(type)
                .contentId(contentId)
                .build();

    }
}
