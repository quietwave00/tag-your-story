package com.tagstory.api.domain.notification.dto.request;

import com.tagstory.core.domain.notification.dto.command.NotificationReadCommand;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationReadRequest {
    private Long notificationId;

    public NotificationReadCommand toCommand(Long userId) {
        return NotificationReadCommand.builder()
                .notificationId(this.notificationId)
                .userId(userId)
                .build();
    }
}
