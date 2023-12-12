package com.tagstory.core.domain.notification.repository;

import com.tagstory.core.domain.notification.NotificationEntity;
import com.tagstory.core.domain.user.UserEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NotificationRepository extends JpaRepository<NotificationEntity, Long> {
    Optional<Page<NotificationEntity>> findBySubscriber(UserEntity userEntity, PageRequest pageRequest);
}
