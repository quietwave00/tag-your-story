package com.tagstory.core.domain.notification.dto.command;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class NotificationReadCommand {
    private Long notificationId;
    private Long userId;
}
