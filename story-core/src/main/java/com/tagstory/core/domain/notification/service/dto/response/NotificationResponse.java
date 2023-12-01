package com.tagstory.core.domain.notification.service.dto.response;

import com.tagstory.core.domain.notification.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {
    private Long userId;

    private NotificationType type;

    private String contentId;
}
