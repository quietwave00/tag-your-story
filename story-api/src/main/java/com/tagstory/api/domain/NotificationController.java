package com.tagstory.api.domain;

import com.tagstory.api.annotations.CurrentUserId;
import com.tagstory.core.domain.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notification")
public class NotificationController {

    private final NotificationService notificationService;

    /*
     * 알림 수신을 위한 SSE를 구독한다.
     */
    @PreAuthorize("hasRole('ROLE_USER')")
    @GetMapping(value = "/subscription", produces = "text/event-stream")
    public SseEmitter subscribe(@CurrentUserId Long userId,
                                @RequestHeader(value = "Last-Event-ID", required = false) String lastEventId) {
        return notificationService.subscribe(userId, lastEventId, LocalDateTime.now());
    }
}
