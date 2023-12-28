package com.tagstory.api.domain.notification;

import com.tagstory.api.annotations.CurrentUserId;
import com.tagstory.api.domain.notification.dto.request.NotificationReadRequest;
import com.tagstory.api.domain.notification.dto.response.NotificationCountResponse;
import com.tagstory.api.domain.notification.dto.response.NotificationResponse;
import com.tagstory.core.domain.notification.service.Notification;
import com.tagstory.core.domain.notification.service.NotificationFacade;
import com.tagstory.core.utils.api.ApiResult;
import com.tagstory.core.utils.api.ApiUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notification")
public class NotificationController {

    private final NotificationFacade notificationFacade;

    /*
     * 알림 수신을 위한 SSE를 구독한다.
     */
    @PreAuthorize("hasRole('USER')")
    @GetMapping(value = "/subscription", produces = "text/event-stream")
    public SseEmitter subscribe(
                                @RequestParam("AccessToken") String token,
                                @CurrentUserId Long userId) {
        return notificationFacade.subscribe(userId, LocalDateTime.now());
    }

    /*
     * 알림 목록을 조회한다.
     */
    @PreAuthorize("hasRole('USER')")
    @GetMapping
    public ApiResult<List<NotificationResponse>> getNotificationList(@CurrentUserId Long userId,
                                                                     @RequestParam int page) {
        List<Notification> notificationList = notificationFacade.getNotificationList(userId, page);
        return ApiUtils.success(notificationList.stream().map(NotificationResponse::from).collect(Collectors.toList()));
    }

    /*
     * 알림 항목을 읽음 처리한다.
     */
    @PreAuthorize("hasRole('USER')")
    @PatchMapping
    public ApiResult<Void> setAsRead(@CurrentUserId Long userId,
                                     @RequestBody NotificationReadRequest request) {
        notificationFacade.setAsRead(request.toCommand(userId));
        return ApiUtils.success();
    }

    /*
     * 알림의 전체 개수를 조회한다.
     */
    @PreAuthorize("hasRole('USER')")
    @GetMapping("/count")
    public ApiResult<NotificationCountResponse> getNotificationCount(@CurrentUserId Long userId) {
        int count = notificationFacade.getNotificationCount(userId);
        return ApiUtils.success(NotificationCountResponse.from(count));
    }

    /*
     * 알림을 모두 읽음 처리한다.
     */
    @PreAuthorize("hasRole('USER')")
    @PatchMapping("/all")
    public ApiResult<Void> setAllAsRead(@CurrentUserId Long userId) {
        notificationFacade.setAllAsRead(userId);
        return ApiUtils.success();
    }
 }
