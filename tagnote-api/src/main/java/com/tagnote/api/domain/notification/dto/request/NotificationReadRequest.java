package com.tagnote.api.domain.notification.dto.request;

import com.tagnote.core.domain.notification.dto.command.NotificationReadCommand;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import io.swagger.v3.oas.annotations.media.Schema;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationReadRequest {
    @Schema(description = "읽음 처리할 알림 ID. 현재 구현은 필수 validation annotation을 사용하지 않는다.", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long notificationId;

    public NotificationReadCommand toCommand(Long userId) {
        return NotificationReadCommand.builder()
                .notificationId(this.notificationId)
                .userId(userId)
                .build();
    }
}
