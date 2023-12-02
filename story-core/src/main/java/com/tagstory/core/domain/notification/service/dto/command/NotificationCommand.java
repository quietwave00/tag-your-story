package com.tagstory.core.domain.notification.service.dto.command;

import com.tagstory.core.domain.notification.NotificationType;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class NotificationCommand {
    /* Subscriber */
    private Long userId;

    /* Publisher */
    private String nickname;

    private NotificationType type;

    private String contentId;



    public static NotificationCommand of(Long userId, String nickname, NotificationType type, String contentId) {
        return NotificationCommand.builder()
                .userId(userId)
                .nickname(nickname)
                .type(type)
                .contentId(contentId)
                .build();
    }
}
