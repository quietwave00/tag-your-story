package com.tagnote.api.domain.notification.dto.response;

import com.tagnote.core.domain.notification.NotificationType;
import com.tagnote.core.domain.notification.service.Notification;
import lombok.Builder;
import lombok.Getter;
import io.swagger.v3.oas.annotations.media.Schema;

@Getter
@Builder
public class NotificationResponse {
    @Schema(description = "알림 ID") private Long notificationId;
    @Schema(description = "대상 게시글 ID") private String contentId;
    @Schema(description = "게시글에 반응한 사용자 표시 이름. 탈퇴자는 '탈퇴한 사용자'") private String pubNickname;
    @Schema(description = "알림 종류: COMMENT 또는 LIKE") private NotificationType type;
    @Schema(description = "읽음 여부") private boolean isRead;

    public static NotificationResponse from(Notification notification) {
        return NotificationResponse.builder()
                .notificationId(notification.getNotificationId())
                .contentId(notification.getContentId())
                .pubNickname(notification.getPublisher().getDisplayNickname())
                .type(notification.getType())
                .isRead(notification.getIsRead())
                .build();
    }
}
