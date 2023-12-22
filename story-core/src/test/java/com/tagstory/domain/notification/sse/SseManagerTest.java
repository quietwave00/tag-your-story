package com.tagstory.domain.notification.sse;

import com.tagstory.core.domain.notification.sse.SseManager;
import com.tagstory.core.domain.notification.sse.SseStorage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@ExtendWith(MockitoExtension.class)
public class SseManagerTest {

    @Mock
    private SseStorage sseStorage;

    @InjectMocks
    private SseManager sseManager;

    @Test
    void SseEmitter를_생성한다() {
        // given
        Long userId = 1L;
        LocalDateTime createdAt = LocalDateTime.now();

        // when
        SseEmitter sseEmitter = sseManager.create(userId, createdAt);

        // then
        assertThat(sseEmitter).isNotNull();
    }

    @Test
    void SseEmitter의_상태를_정의한다() {
        // given
        SseEmitter sseEmitter = Mockito.mock(SseEmitter.class);
        Long key = 1L;

        // when
        sseManager.setUp(sseEmitter, key);

        // then
        Mockito.verify(sseEmitter).onCompletion(Mockito.any(Runnable.class));
        Mockito.verify(sseEmitter).onTimeout(Mockito.any(Runnable.class));
    }
}
