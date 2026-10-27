package com.tagnote.domain.notification.service;

import com.tagnote.core.domain.notification.NotificationEntity;
import com.tagnote.core.domain.user.repository.UserRepository;
import com.tagnote.core.domain.user.UserEntity;
import com.tagnote.core.domain.user.UserStatus;
import com.tagnote.core.domain.notification.dto.command.NotificationReadCommand;
import com.tagnote.core.domain.notification.repository.NotificationRepository;
import com.tagnote.core.domain.notification.service.NotificationService;
import com.tagnote.core.exception.CustomException;
import com.tagnote.domain.notification.fixture.NotificationFixture;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
public class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private UserRepository users;

    @InjectMocks
    private NotificationService notificationService;

    @Test
    void 지연된_알림은_탈퇴한_수신자에게_저장하지_않는다() {
        var notification = NotificationFixture.createNotification(1L);
        when(users.findByUserIdForUpdate(1L)).thenReturn(Optional.of(UserEntity.builder()
                .userId(1L).userStatus(UserStatus.WITHDRAWN).build()));

        assertThat(notificationService.save(notification)).isNull();
        verifyNoInteractions(notificationRepository);
    }

    @Test
    void 알림_읽음_처리시_알림_수신자가_아니면_예외를_발생시킨다() {
        // given
        NotificationReadCommand command = NotificationReadCommand.builder()
                .notificationId(1L)
                .userId(2L) //알림을 확인하려는 유저
                .build();

        // when
        when(notificationRepository.findByNotificationId(command.getNotificationId()))
                .thenReturn(Optional.of(NotificationFixture.createNotificationEntity(1L)));

        // then

        assertThatThrownBy(()->notificationService.setAsRead(command)).isInstanceOf(CustomException.class);
//        try {
//            notificationService.setAsRead(command);
//        } catch(CustomException e) {
//            assertThat(ExceptionCode.NO_READ_PERMISSION).isEqualTo(e.getExceptionCode());
//        }
    }

    @Test
    void 알림_읽음_처리시_알림_수신자가_맞으면_읽음_처리를_한다() {
        // given
        NotificationReadCommand command = NotificationReadCommand.builder()
                .notificationId(1L)
                .userId(1L) //알림을 확인하려는 유저
                .build();
        NotificationEntity notificationEntity = NotificationFixture.createNotificationEntity(1L);

        // when
        when(notificationRepository.findByNotificationId(command.getNotificationId()))
                .thenReturn(Optional.of(notificationEntity));
        notificationService.setAsRead(command);

        // then
        assertThat(notificationEntity.getIsRead()).isTrue();
    }
}
