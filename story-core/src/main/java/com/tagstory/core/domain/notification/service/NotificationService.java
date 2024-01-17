package com.tagstory.core.domain.notification.service;

import com.tagstory.core.domain.notification.NotificationEntity;
import com.tagstory.core.domain.notification.dto.command.NotificationReadCommand;
import com.tagstory.core.domain.notification.repository.NotificationRepository;
import com.tagstory.core.domain.notification.sse.SseManager;
import com.tagstory.core.domain.user.service.User;
import com.tagstory.core.exception.CustomException;
import com.tagstory.core.exception.ExceptionCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

import static com.tagstory.core.domain.notification.properties.NotificationProperties.NOTIFICATION_NAME;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class NotificationService {

    private final NotificationManager notificationManager;
    private final NotificationRepository notificationRepository;
    private final SseManager sseManager;

    public SseEmitter subscribe(Long userId) {
        log.info("SSE subscribe");
        SseEmitter sseEmitter = sseManager.get(userId);

        log.info("Get SSE");
        return Objects.isNull(sseEmitter) ?
                sseManager.create(userId) :
                sseEmitter;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Notification save(Notification notification) {
        return notificationRepository
                .save(notification.toEntity())
                .toNotification();
    }

    public void send(Notification notification) {
        SseEmitter sseEmitter = sseManager.get(notification.getSubscriber().getUserId());
        if(Objects.nonNull(sseEmitter)) {
            try {
                log.info("Send notification");
                sseEmitter.send(SseEmitter.event()
                        .name(NOTIFICATION_NAME)
                        .data(notificationManager.getNotificationData(notification)));
            } catch(IOException e) {
                log.error(e.getMessage());
            }
        }
    }

    public List<Notification> getNotificationList(User user, int page) {
        return findNotificationListByUserId(user, page);
    }

    @Transactional
    public void setAsRead(NotificationReadCommand command) {
        NotificationEntity notificationEntity = getNotificationEntityByNotificationId(command.getNotificationId());
        checkReadPermission(command.getUserId(), notificationEntity);
        notificationEntity.setAsRead();
    }

    public int getNotificationCount(User user) {
        return getNotificationCountBySubscriber(user);
    }

    @Transactional
    public void setAllAsRead(User user) {
        notificationRepository.updateIsReadBySubscriber(user.toEntity());
    }

    /*
     * 단일 메소드
     */
    private List<Notification> findNotificationListByUserId(User user, int page) {
        Page<NotificationEntity> notificationEntityList = notificationRepository
                .findBySubscriberOrderByCreatedAtDesc(user.toEntity(), PageRequest.of(page, 5))
                .orElse(Page.empty());

        return notificationEntityList.stream()
                .map(NotificationEntity::toNotification)
                .toList();
    }

    private Long getUserIdByNotification(NotificationEntity notificationEntity) {
        return notificationEntity.getSubscriber().getUserId();
    }

    private NotificationEntity getNotificationEntityByNotificationId(Long notificationId) {
        return notificationRepository.findByNotificationId(notificationId)
                .orElseThrow(() -> new CustomException(ExceptionCode.NOTIFICATION_NOT_FOUND));
    }

    private void checkReadPermission(Long userId, NotificationEntity notificationEntity) {
        Long subscriberId = getUserIdByNotification(notificationEntity);
        if (!userId.equals(subscriberId)) {
            throw new CustomException(ExceptionCode.NO_READ_PERMISSION);
        }
    }

    private int getNotificationCountBySubscriber(User user) {
        return notificationRepository.countBySubscriber(user.toEntity());
    }
}