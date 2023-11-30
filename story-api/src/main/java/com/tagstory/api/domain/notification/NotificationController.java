package com.tagstory.api.domain.notification;

import com.tagstory.api.annotations.CurrentUserId;
import com.tagstory.api.domain.notification.dto.request.NotificationRequest;
import com.tagstory.core.domain.notification.service.Notification;
import com.tagstory.core.domain.notification.service.NotificationPubService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notification")
public class NotificationController {

    private final NotificationPubService notificationPubService;

    @PostMapping
    public String test(@RequestBody NotificationRequest request, @CurrentUserId Long userId) {
        notificationPubService.sendMessage(request.toCommand(userId));
        return "Success";
    }
}
