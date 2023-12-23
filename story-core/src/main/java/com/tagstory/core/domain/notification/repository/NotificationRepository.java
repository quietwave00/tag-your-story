package com.tagstory.core.domain.notification.repository;

import com.tagstory.core.domain.notification.NotificationEntity;
import com.tagstory.core.domain.user.UserEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<NotificationEntity, Long> {

    Optional<NotificationEntity> findByNotificationId(Long notificationId);

    Optional<Page<NotificationEntity>> findBySubscriberOrderByCreatedAtDesc(UserEntity userEntity, PageRequest pageRequest);

    int countBySubscriber(UserEntity user);

    List<NotificationEntity> findBySubscriber(UserEntity toEntity);

    @Modifying
    @Query("UPDATE NotificationEntity n set n.isRead = true where n.subscriber = :userEntity and n.isRead <> true")
    void updateIsReadBySubscriber(UserEntity userEntity);
}
