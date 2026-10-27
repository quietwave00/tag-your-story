package com.tagnote.core.domain.notification.service;

import com.tagnote.core.domain.notification.NotificationEntity;
import com.tagnote.core.domain.notification.dto.command.NotificationReadCommand;
import com.tagnote.core.domain.notification.repository.NotificationRepository;
import com.tagnote.core.domain.user.service.User;
import com.tagnote.core.domain.user.Role;
import com.tagnote.core.domain.user.UserStatus;
import com.tagnote.core.domain.user.repository.UserRepository;
import com.tagnote.core.exception.CustomException;
import com.tagnote.core.exception.ExceptionCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository users;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Notification save(Notification notification) {
        // Lock the recipient so a delayed event cannot create a notification after withdrawal.
        var subscriber = users.findByUserIdForUpdate(notification.getSubscriber().getUserId());
        if (subscriber.isEmpty() || subscriber.get().getUserStatus() != UserStatus.ACTIVE
                || subscriber.get().getRole() != Role.ROLE_USER) {
            return null;
        }
        if (notification.getPublisher() != null) {
            var publisher = users.findByUserId(notification.getPublisher().getUserId());
            if (publisher.isEmpty() || publisher.get().getUserStatus() != UserStatus.ACTIVE) {
                return null;
            }
        }
        return notificationRepository
                .save(notification.toEntity())
                .toNotification();
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
