package com.tagstory.core.domain.notification.service;

import com.tagstory.core.domain.notification.NotificationType;
import com.tagstory.core.domain.user.service.dto.response.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

    private Long notificationId;

    private User publisher;

    private User subscriber;

    private NotificationType type;

    private String contentId;
}
