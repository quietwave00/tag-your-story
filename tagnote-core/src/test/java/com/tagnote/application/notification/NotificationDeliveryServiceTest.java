package com.tagnote.application.notification;

import com.tagnote.core.domain.notification.NotificationType;
import com.tagnote.core.domain.notification.service.Notification;
import com.tagnote.core.domain.notification.service.NotificationService;
import com.tagnote.core.domain.user.Role;
import com.tagnote.core.domain.user.UserStatus;
import com.tagnote.core.domain.user.UserEntity;
import com.tagnote.core.domain.user.repository.UserRepository;
import com.tagnote.core.domain.user.service.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationDeliveryServiceTest {
    @Mock NotificationService storage;
    @Mock NotificationDeliveryPort port;
    @Mock UserRepository users;
    @InjectMocks NotificationDeliveryService service;

    private Notification notification() {
        User publisher = User.builder().userId(2L).nickname("listener").build();
        User subscriber = User.builder().userId(1L).build();
        return Notification.builder().notificationId(9L).publisher(publisher).subscriber(subscriber)
                .contentId("board-1").type(NotificationType.LIKE).isRead(false).build();
    }

    @Test
    void 알림을_저장한_뒤_최소_필드로_전달한다() {
        Notification saved = notification();
        when(storage.save(saved)).thenReturn(saved);
        when(users.findByUserId(1L)).thenReturn(Optional.of(UserEntity.builder()
                .userId(1L).userStatus(UserStatus.ACTIVE).role(Role.ROLE_USER).build()));

        service.deliver(saved);

        InOrder order = inOrder(storage, users, port);
        order.verify(storage).save(saved);
        order.verify(users).findByUserId(1L);
        order.verify(port).deliver(new NotificationDelivery(9L, 1L, "board-1", "listener", NotificationType.LIKE, false));
    }

    @Test
    void 알림_저장을_건너뛰거나_실패하면_전달하지_않는다() {
        Notification notification = notification();
        when(storage.save(notification)).thenReturn(null).thenThrow(new IllegalStateException("db"));
        service.deliver(notification);
        assertThatCode(() -> service.deliver(notification)).doesNotThrowAnyException();
        verifyNoInteractions(port, users);
    }

    @Test
    void 탈퇴한_수신자에게는_알림을_전달하지_않는다() {
        Notification saved = notification();
        when(storage.save(saved)).thenReturn(saved);
        when(users.findByUserId(1L)).thenReturn(Optional.of(UserEntity.builder()
                .userId(1L).userStatus(UserStatus.WITHDRAWN).build()));
        service.deliver(saved);
        verifyNoInteractions(port);
    }

    @Test
    void 전송에_실패해도_저장된_알림은_유지한다() {
        Notification saved = notification();
        when(storage.save(saved)).thenReturn(saved);
        when(users.findByUserId(1L)).thenReturn(Optional.of(UserEntity.builder()
                .userId(1L).userStatus(UserStatus.ACTIVE).build()));
        doThrow(new IllegalStateException("stream")).when(port).deliver(any());
        assertThatCode(() -> service.deliver(saved)).doesNotThrowAnyException();
        verify(storage).save(saved);
    }
}
