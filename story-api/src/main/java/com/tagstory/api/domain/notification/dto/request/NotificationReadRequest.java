package com.tagstory.api.domain.notification.dto.request;

import com.tagstory.core.domain.notification.dto.command.NotificationReadCommand;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class NotificationReadRequest {
    private Long notificationId;

    public NotificationReadCommand toCommand(Long userId) {
        return NotificationReadCommand.builder()
                .notificationId(this.notificationId)
                .userId(userId)
                .build();
    }
}
