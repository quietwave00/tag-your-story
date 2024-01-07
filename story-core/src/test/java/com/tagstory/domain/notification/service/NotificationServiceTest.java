package com.tagstory.domain.notification.service;

import com.tagstory.core.domain.notification.NotificationEntity;
import com.tagstory.core.domain.notification.dto.command.NotificationReadCommand;
import com.tagstory.core.domain.notification.repository.NotificationRepository;
import com.tagstory.core.domain.notification.service.NotificationManager;
import com.tagstory.core.domain.notification.service.NotificationService;
import com.tagstory.core.domain.notification.sse.SseManager;
import com.tagstory.core.exception.CustomException;
import com.tagstory.domain.notification.fixture.NotificationFixture;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class NotificationServiceTest {

    @Mock
    private NotificationManager notificationManager;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private SseManager sseManager;

    @InjectMocks
    private NotificationService notificationService;

    @Test
    void SseEmitter를_구독시_SseEmitter가_존재하는_경우() {
        // given
        Long userId = 1L;
        SseEmitter mockSseEmitter = mock(SseEmitter.class);

        // when
        when(sseManager.get(userId)).thenReturn(mockSseEmitter);
        SseEmitter result = notificationService.subscribe(userId);

        // then
        assertThat(result).isEqualTo(mockSseEmitter);
    }

    @Test
    void SseEmitter를_구독시_SseEmitter가_존재하지_않는_경우() {
        // given
        Long userId = 1L;
        SseEmitter mockSseEmitter = mock(SseEmitter.class);

        // when
        when(sseManager.get(userId)).thenReturn(null);
        when(sseManager.create(userId)).thenReturn(mockSseEmitter);
        SseEmitter result = notificationService.subscribe(userId);

        // then
        assertThat(result).isEqualTo(mockSseEmitter);
    }

    @Test
    void SseEmitter로_메시지를_보낸다() throws IOException {
//        // given
//        Notification notification = NotificationFixture.createNotification();
//        String notificationData = new ObjectMapper().writeValueAsString(notification);
//        SseEmitter mockSseEmitter = mock(SseEmitter.class);
//
//        // when
//        when(sseManager.get(notification.getSubscriber().getUserId())).thenReturn(mockSseEmitter);
//        when(notificationManager.getNotificationData(notification))
//                .thenReturn(notificationData);
//        notificationService.send(notification);
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
