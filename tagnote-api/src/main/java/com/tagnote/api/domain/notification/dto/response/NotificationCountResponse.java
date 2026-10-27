package com.tagnote.api.domain.notification.dto.response;

import lombok.Builder;
import lombok.Getter;
import io.swagger.v3.oas.annotations.media.Schema;

@Getter
@Builder
public class NotificationCountResponse {
    @Schema(description = "읽음 여부와 관계없는 수신자의 전체 알림 수")
    int count;

    public static NotificationCountResponse from(int count) {
        return builder()
                .count(count)
                .build();
    }
}
