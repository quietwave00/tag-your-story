package com.tagstory.core.domain.notification.repository;

import com.tagstory.core.domain.notification.NotificationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<NotificationEntity, Long> {
}
