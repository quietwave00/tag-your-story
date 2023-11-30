package com.tagstory.core.domain.notification.service.dto.command;

import com.tagstory.core.domain.notification.NotificationType;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class NotificationCommand {
    private Long userId;

    private NotificationType type;

    private String contentId;
}
