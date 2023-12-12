package com.tagstory.core.domain.notification.service;

import com.tagstory.core.domain.notification.NotificationEntity;
import com.tagstory.core.domain.notification.NotificationType;
import com.tagstory.core.domain.notification.repository.NotificationRepository;
import com.tagstory.core.domain.notification.service.dto.command.NotificationCommand;
import com.tagstory.core.domain.notification.sse.SseManager;
import com.tagstory.core.domain.user.service.dto.response.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationManager notificationManager;
    private final NotificationRepository notificationRepository;
    private final SseManager sseManager;


    /* pub/sub 구현 방법 */
    public void notifyComment(Long userId, String nickname, String contentId) {
        NotificationCommand command = NotificationCommand.of(userId, nickname, NotificationType.COMMENT, contentId);
        notificationManager.sendMessage(command);
    }

    /* eventPublisher 구현 방법 */
    public SseEmitter subscribe(Long userId, String lastEventId, LocalDateTime createdAt) {
        return sseManager.create(userId, createdAt);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void save(Notification notification) {
        notificationRepository.save(notification.toEntity());
    }

    public void send(Notification notification) {
        SseEmitter sseEmitter = sseManager.get(notification.getSubscriber().getUserId());
        try {
            sseEmitter.send(SseEmitter.event()
                    .name("Notification")
                    .data(notificationManager.getNotificationData(notification)));
        } catch(IOException e) {
            log.error(e.getMessage());
        }
    }

    public List<Notification> getNotificationList(User user, int page) {
        return findNotificationListByUserId(user, page);
    }

    /*
     * private
     */
    public List<Notification> findNotificationListByUserId(User user, int page) {
        Page<NotificationEntity> notificationEntityList = notificationRepository.findBySubscriber(user.toEntity(), PageRequest.of(page, 5))
                .orElse(Page.empty());

        return notificationEntityList.stream()
                .map(NotificationEntity::toNotification)
                .collect(Collectors.toList());
    }
}